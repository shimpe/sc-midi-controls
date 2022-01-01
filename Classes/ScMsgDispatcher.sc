ScMsgDispatcher {

	var <>midi_device_name;
	var <>midi_port_name;
	var <>midi_out_latency;
	var <>midi_out;

	var <>observers;
	var <>learning_observers;

	var <>cc_responder;
	var <>nrpn_responder;
	var <>bend_responder;

	*new {
		^super.new.init();
	}

	init {
		this.midi_device_name = "";
		this.midi_port_name = "";
		this.midi_out_latency = Server.default.latency;
		this.midi_out = nil;
		this.observers = IdentityDictionary();
		this.learning_observers = IdentityDictionary();
		this.cc_responder = nil;
		this.nrpn_responder = nil;
		this.bend_responder = nil;
	}

	connect {
		|  midi_device_name, midi_port_name, midi_out_latency=nil |
		var found = false;

		this.midi_device_name = midi_device_name;
		this.midi_port_name = midi_port_name;
		this.midi_out_latency = midi_out_latency;

		if (MIDIClient.initialized.not) { MIDIClient.init; };

		if (MIDIClient.initialized.not) {
			MIDIClient.init;
		};
		MIDIClient.destinations.do {
			|el, idx|
			if (el.name.debug("check midi endpoint looking for %".format(midi_port_name)) == midi_port_name) {
				("connecting to midi endpoint " ++ el.name).postln;
				found = true;
				this.midi_out = MIDIOut.newByName(midi_device_name, el.name).latency_(
					this.midi_out_latency ?? {Server.default.latency});
			};
		};
		if (found.not) {
			"WARNING: couldn't find MIDI endpoint " ++ midi_device_name ++ " " ++ midi_port_name ++ ". Connect failed!.".postln;
		};
		MIDIIn.connectAll;
		this.initCcResponder;
		this.initNrpnResponder;
		this.initBendResponder;
	}

	learn {
		| what |
		this.learning_observers[what.uniquename.asSymbol] = what;
		what.obsspec = nil; // force recalibration
	}

	stopLearning {
		// transfer learned observer to observers
		this.learning_observers.keysValuesDo({
			| key, value |
			this.observers[key.asSymbol] = value;
		});
		// remove learned observers
		this.learning_observers = IdentityDictionary();
	}

	notifyObservers {
		| obstype, observers, src, chan, incomingNum, incomingVal |
		this.observers.do {
			| observer |
			observer.debug("observer");
			observer.obsspec.debug("obsspec");
			if ((observer.obstype == obstype).and(
				observer.obsctrl == incomingNum).and(
				(observer.obsspec.minval <= incomingVal || observer.obsspec.minval.isNil)).and(
				(observer.obsspec.maxval >= incomingVal|| observer.obsspec.maxval.isNil)).and(
				(observer.obschan == chan || observer.obschan.isNil)).and(
				(observer.obssrc == src || observer.obssrc.isNil))) {
				if (observer.muted.not) {
					observer.receivePrivate(this, observer, src, chan, incomingNum, incomingVal);
					observer.receivePublic(this, observer, src, chan, incomingNum, incomingVal);
				}
			};
		};
	}

	updateLearningObservers {
		| obstype, learning_observers, src, chan, incomingNum, incomingVal, default_max_value |
		// if there are any controllers waiting to learn this CC, now is the time
		learning_observers.do {
			| learning_observer |
			var old_ctrl = learning_observer.obsctrl;
			var ctrl_change = old_ctrl != incomingNum;
			learning_observer.obstype = obstype;
			learning_observer.obssrc = src;
			learning_observer.obsctrl = incomingNum;
			learning_observer.obschan = chan;
			if (ctrl_change || learning_observer.obsspec.isNil) {
				learning_observer.obsspec = ControlSpec(minval:0, maxval:default_max_value, step:1, default:0, units:"");
			} /*else*/ {
				if (incomingVal < learning_observer.obsspec.minval) {
					learning_observer.obsspec.minval = incomingVal;
				};
				if (incomingVal > learning_observer.obsspec.maxval) {
					learning_observer.obsspec.maxval = incomingVal;
				};
			};
		};

		// transfer learned observer to observers
		this.learning_observers.keysValuesDo({
			| key, value |
			this.observers[key.asSymbol] = value;
		});
	}

	initCcResponder {
		| ignore_nrpn=true |
		this.cc_responder = CCResponder({ |src, chan, num, val|
			if ( #[99, 98, 6, 38].includes(num) || ignore_nrpn.not) {
				// skip
			} /*else*/ {
				// if there are any controllers waiting to learn this CC, now is the time
				this.updateLearningObservers(\cc, this.learning_observers, src, chan, num, val);

				// notify all relevant observers that this CC was received
				this.notifyObservers(\cc, this.observers, src, chan, num, val);
			};
		});
	}

	initNrpnResponder {
		var list = #[99, 98, 6, 38];
		var seq = Pseq(list, 1).asStream;
		var incomingNum = 0;
		var incomingVal = 0;

		this.nrpn_responder = CCResponder({
			|src, chan, num, val|
			var nextseq = seq.next;
			if(num == nextseq) {
				switch(num)
				{ 99 } {
					incomingNum = incomingNum | (val << 7)
				}
				{ 98 } {
					incomingNum = incomingNum | val
				}
				{ 6 } {
					incomingVal = incomingVal | (val << 7)
				}
				{ 38 } {
					incomingVal = incomingVal | val;

					// if there are any controllers waiting to learn this NRPN, now is the time
					this.updateLearningObservers(
						\nrpn,
						this.learning_observers,
						src,
						chan,
						incomingNum,
						incomingVal,
						0);

					// notify all relevant observers that this CC was received
					this.notifyObservers(\nrpn, this.observers, src, chan, incomingNum, incomingVal);

					incomingNum = incomingVal = 0;
					seq.reset;
				};
			} {
				seq.reset
			};
		}, num: list);
	}

	initBendResponder {
		this.bend_responder = BendResponder({ |src, chan, val|
			// if there are any controllers waiting to learn this CC, now is the time
			this.updateLearningObservers(\bend, this.learning_observers, src, chan, "BEND", val, 16384);

			// notify all relevant observers that this CC was received
			this.notifyObservers(\bend, this.observers, src, chan, "BEND", val);
		});
	}

	cleanUp {
		this.cc_responder.remove;
		this.nrpn_responder.remove;
		this.bend_responder.remove;
	}
}