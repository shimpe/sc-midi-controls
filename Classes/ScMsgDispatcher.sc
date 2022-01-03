ScMsgDispatcher {

	var <>midi_device_name;
	var <>midi_port_name;
	var <>midi_out_latency;
	var <>midi_out;

	var <>observers;
	var <>learning_observers;

	var <>log_responder;
	var <>cc_responder;
	var <>rpn_responder;
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
		this.log_responder = nil;
		this.cc_responder = nil;
		this.rpn_responder = nil;
		this.nrpn_responder = nil;
		this.bend_responder = nil;
	}

	sendCc {
		| chan, control, value |
		this.midi_out.control(chan, control, value.asInteger);
	}

	sendRpn {
		| chan, control, value |
		var cCC_MSB = 101; // 0x65
		var cCC_LSB = 100; // 0x64
		var cDATA_MSB = 6; // 0x06
		var cDATA_LSB = 38; // 0x26
		var number_msb = control >> 7;
		var number_lsb = control & 127;
		var intval = value.asInteger;
		var value_msb = intval >> 7;
		var value_lsb = intval & 127;
		this.midi_out.control(chan, cCC_LSB, number_lsb);
		this.midi_out.control(chan, cCC_MSB, number_msb);
		this.midi_out.control(chan, cDATA_LSB, value_lsb);
		this.midi_out.control(chan, cDATA_MSB, value_msb);
		this.midi_out.control(chan, cCC_LSB, 0x7F);
		this.midi_out.control(chan, cCC_MSB, 0x7F);
	}

	sendNrpn {
		| chan, control, value |
		var cCC_MSB = 99;
		var cCC_LSB = 98;
		var cDATA_MSB = 6;
		var cDATA_LSB = 38;
		var number_msb = control >> 7;
		var number_lsb = control & 127;
		var intval = value.asInteger;
		var value_msb = intval >> 7;
		var value_lsb = intval & 127;
		this.midi_out.control(chan, cCC_MSB, number_msb);
		this.midi_out.control(chan, cCC_LSB, number_lsb);
		this.midi_out.control(chan, cDATA_MSB, value_msb);
		this.midi_out.control(chan, cDATA_LSB, value_lsb);
	}

	sendBend {
		| chan, value |
		this.midi_out.bend(chan, value.asInteger);
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
		this.initLogResponder;
		this.initCcResponder;
		this.initRpnResponder;
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
			if ((observer.obstype == obstype).and(
				(observer.obsctrl == incomingNum) || (observer.obsctrl.isNil)).and(
				(observer.obsspec.minval <= incomingVal || observer.obsspec.minval.isNil)).and(
				(observer.obsspec.maxval >= incomingVal|| observer.obsspec.maxval.isNil)).and(
				(observer.obschan == chan || observer.obschan.isNil)).and(
				(observer.obssrc == src || observer.obssrc.isNil))) {
				observer.receivePrivate(this, observer, src, chan, incomingNum, incomingVal);
				if (observer.muted.not) {
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

	initLogResponder {
		this.log_responder = CCResponder({
			| src, chan, num, val |
			this.notifyObservers(\log, this.observers, src, chan, num, val);
		});
	}

	initCcResponder {
		| ignore_rpn_nrpn=true |
		this.cc_responder = CCResponder({ |src, chan, num, val|
			if ( #[101, 100, 99, 98, 6, 38].includes(num) || ignore_rpn_nrpn.not) {
				// skip
			} /*else*/ {
				// if there are any controllers waiting to learn this CC, now is the time
				this.updateLearningObservers(\cc, this.learning_observers, src, chan, num, val);

				// notify all relevant observers that this CC was received
				this.notifyObservers(\cc, this.observers, src, chan, num, val);
			};
		});
	}

	initRpnResponder {
		var list = #[101, 100, 6, 38];
		var seq = Pseq(list, 1).asStream;
		var incomingNum = 0;
		var incomingVal = 0;
		"initing rpn".postln;
		this.nrpn_responder = CCResponder({
			|src, chan, num, val|
			var nextseq = seq.next;
			if(num == nextseq) {
				switch(num)
				{ 101 } {
					incomingNum = incomingNum | (val << 7)
				}
				{ 100 } {
					incomingNum = incomingNum | val
				}
				{ 6 } {
					incomingVal = incomingVal | (val << 7)
				}
				{ 38 } {
					incomingVal = incomingVal | val;

					// if there are any controllers waiting to learn this NRPN, now is the time
					this.updateLearningObservers(
						\rpn,
						this.learning_observers,
						src,
						chan,
						incomingNum,
						incomingVal,
						0);

					// notify all relevant observers that this CC was received
					this.notifyObservers(\rpn, this.observers, src, chan, incomingNum, incomingVal);

					incomingNum = incomingVal = 0;
					seq.reset;
				};
			} {
				seq.reset
			};
		}, num: list);
	}

	initNrpnResponder {
		var list = #[99, 98, 6, 38];
		var seq = Pseq(list, 1).asStream;
		var incomingNum = 0;
		var incomingVal = 0;
		"initing nrpn".postln;
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

	refreshUI {
		this.observers.do {
			| observer |
			observer.refreshUI;
		};
	}

	cleanUp {
		this.cc_responder.remove;
		this.rpn_responder.remove;
		this.nrpn_responder.remove;
		this.bend_responder.remove;
	}
}