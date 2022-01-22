/*
[general]
title = "ScMsgDispatcher"
summary = "a data structure that manages all midi controls and centralizes all midi communication"
categories = "Midi Utils"
related = "Classes/ScMidiTextField, Classes/ScNumericControl, Classes/ScMidiSlider, Classes/ScMidiKnob, Classes/ScMidiControlChangeDumper"
description = '''
ScMsgDispatcher is a central class in the whole framework. It maintains lists of instantiated controls (stored by their unique name) and it knows how to receive/parse and send midi control changes. It sets up midi listeners, and notifies registered controls of incoming messages. Inside your program, you need to create one ScMsgDispatcher and pass it to every control you create.
'''
*/
ScMsgDispatcher {
	/*
	[method.midi_device_name]
	description = "Name of the midi device to which the ScMsgDispatcher is connected. This corresponds to the first string in a MIDIClient.destinations entry."
	[method.midi_device_name.returns]
	what = "a string with the midi device name. Gets initialized during call to 'connect'."
	*/
	var <>midi_device_name;
	/*
	[method.midi_port_name]
	description = "Midi port to which the ScMsgDispatcher is connected. This corresponds to the second string in a MIDIClient.destinations entry."
	[method.midi_port_name.returns]
	what = "a string with the midi port name. Gets initialized during call to 'connect'."
	*/
	var <>midi_port_name;
	/*
	[method.midi_out_latency]
	description = "Midi out latency. Use 0 if talk to an external synth. Use Server.default.latency if you do use internal synthesis. Defaults to nil, which is translated to Server.default.latency. This gets initialized during the call to 'connect'."
	[method.midi_out_latency.returns]
	what = "an integer midi_out_latency"
	*/
	var <>midi_out_latency;
	/*
	[method.midi_out]
	description = "MIDIOut to which the ScMsgDispatcher is connected"
	[method.midi_out.returns]
	what = "a MIDIOut instance. this gets initialized during the call to 'connect'"
	*/
	var <>midi_out;
	/*
	[method.observers]
	description = "List of midi controls that are configured and bound to some midi controller"
	[method.observers.returns]
	what = "a list of midi controls"
	*/
	var <>observers;
	/*
	[method.learning_observers]
	description = "list of midi controls that are currently in learning mode. Controls in learning mode update their src, channel, type and spec as control changes are received. When you press a learn button in the ui, the corresponding control is added into this list automatically. During learning, the control is also already added to the normal observers, so it can interactively update its visualization based on what it is learning."
	[method.learning_observers.returns]
	what = "a list of midi controls that are currently in learning mode"
	*/
	var <>learning_observers;
	/*
	[method.cc_responder]
	description = "internal CCResponder that listens for control changes"
	[method.cc_responder.returns]
	what = "a CCResponder"
	*/
	var <>cc_responder;
	/*
	[method.rpn_responder]
	description = "internal CCResponder that listens for RPN"
	[method.rpn_responder.returns]
	what = "a CCResponder"
	*/
	var <>rpn_responder;
	/*
	[method.nrpn_responder]
	description = "internal CCResponder that listens for NRPN"
	[method.nrpn_responder.returns]
	what = "a CCResponder"
	*/
	var <>nrpn_responder;
	/*
	[method.bend_responder]
	description = "internal BendResponder that listens for pitch bend msgs"
	[method.bend_responder.returns]
	what = "a BendResponder"
	*/
	var <>bend_responder;
    /*
	[method.programchange_responder]
	description = "internal CCResponder that listens for pitch program changes"
	[method.programchange_responder.returns]
	what = "a CCResponder"
	*/
	var <>programchange_responder;


	/*
	[method.sysex_responder]
	description = "internel sysex listener"
	[method.sysex_responder.returns]
	what = "a sysex responder"
	*/
	var <>sysex_responder;


	/*
	[classmethod.new]
	description = "creates a new ScMsgDispatcher"
	[classmethod.new.returns]
	what = "a new ScMsgDispatcher"
	*/
	*new {
		^super.new.init();
	}

	/*
	[method.init]
	description = "initializes a new ScMsgDispatcher"
	[method.init.returns]
	what = "an initialized ScMsgDispatcher (note: initialized is not the same as connected to a midi device)"
	*/
	init {
		this.midi_device_name = "";
		this.midi_port_name = "";
		this.midi_out_latency = Server.default.latency;
		this.midi_out = nil;
		this.observers = IdentityDictionary();
		this.learning_observers = IdentityDictionary();
		this.cc_responder = nil;
		this.rpn_responder = nil;
		this.nrpn_responder = nil;
		this.bend_responder = nil;
		this.programchange_responder = nil;
		this.sysex_responder = nil;
	}

	/*
	[method.sendCc]
	description = "sends a midi CC (control change) to the midi device. Used by controls of type \\cc that want to send a value to the midi device."
	[method.sendCc.args]
	chan = "midi channel"
	control = "midi controller number"
	value = "midi controller value"
	*/
	sendCc {
		| chan, control, value |
		this.midi_out.control(chan, control, value.asInteger);
	}

	/*
	[method.sendRpn]
	description = "sends an RPN message to the midi device. Used by controls of type \rpn that want to send a value to the midi device."
	[method.sendRpn.args]
	chan = "midi channel"
	control = "midi controller number"
	value = "midi controller value"
	*/
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

	/*
	[method.sendNrpn]
	description = "sends an NRPN message to the midi device. Used by controls of type \nrpn that want to send a value to the midi device."
	[method.sendNrpn.args]
	chan = "midi channel"
	control = "midi controller number"
	value = "midi controller value"
	*/
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

	/*
	[method.sendBend]
	description = "sends a pitch bend message to the midi device. Used by controls of type \bend"
	[method.sendBend.args]
	chan = "midi channel"
	value = "bend value (0..16383)"
	*/
	sendBend {
		| chan, value |
		this.midi_out.bend(chan, value.asInteger);
	}

	/*
	[method.sendBank]
	description = "sends a bank select msg to the midi device."
	[method.sendBank.args]
	chan = "midi channel"
	bank = "bank value"
	*/
	sendBank {
		| chan, bank |
		this.midi_out.control(chan, 0, bank);
	}

	/*
	[method.sendProgramChange]
	description = "sends a program change msg to the midi device. Used by controls of type \\prog"
	[method.sendProgramChange.args]
	chan = "midi channel"
	patch = "patch value"
	*/
	sendProgramChange {
		| chan, patch |
		this.midi_out.program(chan, patch);
	}


	/*
	[method.sendBankAndProgramChange]
	description = "sends a bank select followed by a program change msg to the midi device."
	[method.sendBankAndProgramChange.args]
	chan = "midi channel"
	bank = "bank value"
	patch = "patch value"
	*/
	sendBankAndProgramChange {
		| chan, bank, patch |
		this.sendBank(chan, bank);
		this.sendProgramChange(chan, patch);
	}

	/*
	[method.connect]
	description = "connect initializes the midi system and connects to a specific midi device"
	[method.connect.args]
	midi_device_name = "Name of the midi device to which the ScMsgDispatcher is connected. This corresponds to the first string in a MIDIClient.destinations entry."
	midi_port_name = "Midi port to which the ScMsgDispatcher is connected. This corresponds to the second string in a MIDIClient.destinations entry."
	midi_out_latency = "Midi out latency. Use 0 if talk to an external synth. Use Server.default.latency if you do use internal synthesis. Defaults to nil, which is translated to Server.default.latency. Use 0 if you talk to external synths."
	*/
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
		this.initRpnResponder;
		this.initNrpnResponder;
		this.initBendResponder;
		this.initProgramchangeResponder;
		this.initSysexResponder;
	}

	/*
	[method.learn]
	description = "learn adds a midi control to the list of learning controls; learning controls update their specs as control change (or pitch bend, program change) messages are received"
	[method.learn.args]
	what = "a midi control that will start learning"
	*/
	learn {
		| what |
		this.learning_observers[what.uniquename.asSymbol] = what;
		what.obsspec = nil; // force recalibration
	}

	/*
	[method.stopLearning]
	description = "stops all learning and transfers the learning controls to the list of known midi controls"
	*/
	stopLearning {
		// transfer learned observer to observers
		this.learning_observers.keysValuesDo({
			| key, value |
			this.observers[key.asSymbol] = value;
		});
		// remove learned observers
		this.learning_observers = IdentityDictionary();
	}

	/*
	[method.notifyObservers]
	description = '''
	Internal method that notifies all known midi controls of an incoming control change.
	All controllers (muted and unmuted) will have their receivePrivate method called.
	Only unmuted controllers will have their receivePublic method called.
	Normally control changes are only sent to "compatible" controls.
	E.g. a midi control that is bound to an NRPN message will only get notifications if the nrpn it is
	listening to came in. A control of type \log is compatible with all recognized control changes.
	'''
	[method.notifyObservers.args]
	obstype = "type of control change that was detected (\\cc, \\rpn, \\nrpn, \\bend, \\prog)"
	observers = "list of known controls in the system"
	src = "midi src of control change"
	chan = "midi channel of control change"
	incomingNum = "midi controller number of incoming change (or \"BEND\" or \"PROG\"))"
	incomingVal = "midi controller value of incoming change"
	*/
	notifyObservers {
		| obstype, observers, src, chan, incomingNum, incomingVal |
		this.observers.do {
			| observer |
			if ((observer.obstype == obstype || (observer.obstype == \log)).and(
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

	/*
	[method.notifyObserversSysex]
	description = "method to dispatch received sysex to observer"
	[method.notifyObserversSysex.args]
	observers = "list of observers to potentially notify"
	src = "midi src"
	data = "sysex data as Int8Array"
	*/
	notifyObserversSysex {
		| observers, src, data |
		observers.do {
			| observer |
			observer.receivePrivateSysex(this, observer, src, data);
		};
	}

	/*
	[method.notifyControlSendPreviousValue]
	description = '''
	Asks control with a given unique name to resend its last received value.
	This is typically used by coupled controls, e.g. to resend a bank select if a program change control is adapted.
	'''
	[method.notifyControlSendPreviousValue.args]
	unique_name = "unique name of the control"
	*/
	notifyControlSendPreviousValue {
		| unique_name |
		if (observers[unique_name.asSymbol].notNil) {
			observers[unique_name.asSymbol].sendPreviousValue();
		}
	}

	/*
	[method.updateLearningObservers]
	description = "Internal method that adapts the learning observer with the incoming control changes. The learning controls are also added to the list of known controls so they can interactive visualize their internal state as they are learning."
	[method.updateLearningObservers.args]
	obstype = "type of incoming control change (\\cc, \\rpn, \\nrpn, \\bend, \\prog)"
	learning_observers = "list of controls that are currently in learning mode"
	src = "midi src of incoming control change"
	chan = "midi channel of incoming control change"
	incomingNum = "midi controller number of incoming control change (or \"BEND\" or \"PROG\")"
	incomingVal = "midi controller value of incoming control change"
	default_max_value = '''
	Default max value to use when learning a new controller.
	0 will force range calibration (i.e. user is supposed to send complete range of possible values during learning).
	'''
	*/
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


	/*
	[method.extractObserverProperties]
	description = "Internal method. Extracts properties from a midi control that can be used to save the state of the control to file into an IdentityDictionary"
	[method.extractObserverProperties.args]
	observer = "midi control"
	[method.extractObserverProperties.returns]
	what = "a list with 2 elements: element[0] is the observer's unique name and element[1] is the dictionary with properties"
	*/
	extractObserverProperties {
		| observer |
		var props = IdentityDictionary;
		observer.extractProperties(props);
		^[observer.uniquename, props];
	}

	/*
	[method.save]
	description = "saves the state of all known controls to disk. state is stored per control based on unique name of the control"
	[method.save.args]
	filename = "file name to save"
	*/
	save {
		| filename |
		var properties = IdentityDictionary();
		this.observers.do {
			|observer|
			var props = this.extractObserverProperties(observer);
			properties[ props[0].asSymbol ] = props[1];
		};
		Archive.global.put(\msgDispatcherState, properties);
		Archive.write(filename);
	}

	/*
	[method.load]
	description = "loads the state of all known controls from disk, based on unique name to set loaded info into system state"
	[method.load.args]
	filename = "file name to load"
	*/
	load {
		| filename |
		var properties;
		Archive.load(filename);
		properties = Archive.global.at(\msgDispatcherState);
		properties.keysValuesDo {
			| key, props |
			if (this.observers[key.asSymbol].notNil) {
				this.observers[key.asSymbol].initFromProperties(props);
			} /*else*/ {
				("Observer with unique key " ++ key.asString ++ " doesn't exist. Load skipped.").warn;
			};
		};
	}


	/*
	[method.initCcResponder]
	description = "Internal method to set up a CcResponder. By default some controller numbers are ignored (because they are used in RPN or NRPN messages."
	[method.initCcResponder.args]
	ignore_rpn_nrpn = "default true, which means that this CcResponder will not fire if a controller numer used for rpn or nrpn is received"
	*/
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

	/*
	[method.initRpnResponder]
	description = "Internal method to set up an RPN Responder."
	*/
	initRpnResponder {
		var list = #[101, 100, 6, 38];
		var seq = Pseq(list, 1).asStream;
		var incomingNum = 0;
		var incomingVal = 0;
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


	/*
	[method.initNrpnResponder]
	description = "Internal method to set up an NRPN Responder."
	*/
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

	/*
	[method.initBendResponder]
	description = "Internal method to set up a pitch bend responder."
	*/
	initBendResponder {
		this.bend_responder = BendResponder({ |src, chan, val|
			// if there are any controllers waiting to learn this CC, now is the time
			this.updateLearningObservers(\bend, this.learning_observers, src, chan, "BEND", val, 16384);

			// notify all relevant observers that this CC was received
			this.notifyObservers(\bend, this.observers, src, chan, "BEND", val);
		});
	}

	/*
	[method.initProgramchangeResponder]
	description = "Internal method to set up an initProgramChangeResponder"
	*/
	initProgramchangeResponder {
		this.programchange_responder = ProgramChangeResponder({ | src, chan, val |
			// if there are any controllers waiting to learn a program change, now is the time
			this.updateLearningObservers(\prog, this.learning_observers, src, chan, "PROG", val, 127);

			// notify all relevant observers that this change was received
			this.notifyObservers(\prog, this.observers, src, chan, "PROG", val);
		});
	}

	/*
	[method.initSysexResponder]
	description = "Internal method to set up a sysex responder"
	*/
	initSysexResponder {
		this.sysex_responder = MIDIFunc.sysex({
			| data, src |
			this.notifyObserversSysex(this.observers, src, data);
		});
	}

	/*
	[method.refreshUI]
	description = "Method that can be called to ask all known midi controls to update their label"
	*/
	refreshUI {
		this.observers.do {
			| observer |
			observer.refreshUI;
		};
	}

	/*
	[method.cleanUp]
	description = "method that removes all created reponders"
	*/
	cleanUp {
		this.cc_responder.remove;
		this.rpn_responder.remove;
		this.nrpn_responder.remove;
		this.bend_responder.remove;
		this.programchange_responder.remove;
		this.sysex_responder.remove;
	}
}