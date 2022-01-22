/*
[general]
title = "ScNumericControl"
summary = "base class for numeric midi controls"
categories = "Midi Utils"
related = "Classes/ScMsgDispatcher, Classes/ScMidiTextField, Classes/ScMidiKnob, Classes/ScMidiSlider, Classes/ScControlChangeDumper"
description = '''
Base class for the concrete controls like ScMidiTextField, ScMidiKnob, ScMidiSlider. This class contains everything that is common between different kinds of bidirectional midi controls that hold a single numerical value. Normally you will never instantiate a ScNumericControl directly. It contains no ui controls.
'''
*/
ScNumericControl {

	/*
	[method.uniquename]
	description = "encapsulates the unique name for this control. unique name is used internally and must be unique over all controls. unique name is passed to the control in the constructor"
	[method.uniquename.returns]
	what = "a unique name (string)"
	*/
	var <>uniquename;
	/*
	[method.gui_name]
	description = "encapsulates the gui name for this control. gui name can be shared with other controls and is used to make a label. gui_name is passed to the control in the constructor."
	[method.gui_name.returns]
	what = "a gui name (string)"
	*/
	var <>gui_name;
	/*
	[method.obschan]
	description = "encapsulates this control's midi channel. This is initialized during prebinding or during learning."
	[method.obschan.returns]
	what = "a midichannel (Integer)"
	*/
	var <>obschan;
	/*
	[method.obstype]
	description = '''
	Encapsulates this control's controller type. This is initialized during prebinding or during learning.
	At the moment of writing this can be one of \cc, \bend, \rpn, \nrpn, \log.
	'''
	[method.obstype.returns]
	what = "a control type"
	*/
	var <>obstype;
	/*
	[method.obsspec]
	description = '''
	Encapsulates this control's ControlSpec (min value, max value, default value).
	This is initialized during prebinding or during learning.
	'''
	[method.obsspec.returns]
	what = "a ControlSpec"
	*/
	var <>obsspec;
	/*
	[method.obssrc]
	description = '''
	Encapsulates this control's midi src.
	This is initialized during prebinding or during learning.
	'''
	[method.obssrc.returns]
	what = "a midi src (Integer)"
	*/
	var <>obssrc;
	/*
	[method.obsctrl]
	description = '''
	Encapsulates this control's midi controller number.
	This is initialized during prebinding or during learning.
	'''
	[method.obsctrl.returns]
	what = 'a controller number (or "BEND" or "PROG")'
	*/
	var <>obsctrl;
	/*
	[method.msg_dispatcher]
	description = '''
	Encapsulates this control's midi msg dispatcher.
	This is passed to the control in the constructor.
	'''
	[method.msg_dispatcher.returns]
	what = "a msgDispatcher, responsible for all midi communication"
	*/
	var <>msg_dispatcher;
	/*
	[method.receive_handler]
	description = '''
	Encapsulates this control's custom receive handler.
	This is optionally set up by the end user, using the "registerReceiveHandler" call.
	The custom receive handler must be a function taking 6 arguments.
	These 6 arguments are: 1. msgDispatcher, 2. the ScMidiNumericControl itself, 3. the midi src,
	4. the midi channel, 5. the controller number (for pitch bending this is a string "BEND", for program change this
	is a string "PROG") and 6. the controller value that is received
	'''
	[method.receive_handler.returns]
	what = "a function"
	*/
	var <>receive_handler;
	/*
	[method.value_lookup_table]
	description = '''
	Encapsulates this control's value_lookup_table. If a value_lookup_table is set up (this is optional), it will be used to translate the midi values into symbolic names which are displayed in the label (if the label is visible). See the bigsky example for an illustration.
	'''
	[method.value_lookup_table.returns]
	what = "an array where index in the array corresponds to midi controller value, and entry in the array corresponds to the corresponding symbolic value"
	*/
	var <>value_lookup_table;
	/*
	[method.previous_value]
	description = '''
	Encapsulates this control's last received value, in case it needs to resend its value at some point.
	'''
	[method.previous_value.returns]
	what = "last received controller value"
	*/
	var <>previous_value;
	/*
	[method.list_of_presend_coupled_controls]
	description = '''
	A list of unique names of other controls in the system that need to send their value before this control can send its value. Example would be having two controls, one for bank number and one for program select. When program select is modified, also bank number has to be sent again.
	'''
	[method.list_of_presend_coupled_controls.returns]
	what = "a list of unique names"
	*/
	var <>list_of_presend_coupled_controls;
	/*
	[method.list_of_postsend_coupled_controls]
	description = '''
	A list of unique names of other controls in the system that need to send their value before this control can send its value. Example would be having two controls, one for bank number and one for program select. When bank number is modified, also program select has to be resent.
	'''
	[method.list_of_postsend_coupled_controls.returns]
	what = "a list of unique names"
	*/
	var <>list_of_postsend_coupled_controls;
	/*
	[method.custom_control_action]
	description = '''
	An optional action triggered when the user modifies the control in the supercollider UI. Sending control changes to the midi device is handled automatically by the framework and doesn't need any custom control action to be defined. The custom_control_action receives the view (e.g. a knob or a slider) as argument so you can interpret the value.
	'''
	[method.custom_control_action.returns]
	what = "a function expecting a view argument filled in by the framework"
	*/
	var <>custom_control_action;

	/*
	[classmethod.new]
	description = "creates a new ScNumericControl"

	[classmethod.new.args]
	unique_name = '''
	Every ScNumericControl needs a unique name.
	This name is configured at the time of instantiating the control (typically in the constructor arguments).
	'''
	gui_name = '''
	Every ScNumericControl needs a gui name. This is a name that is shown in the label, if the label is visible.
	Multiple controls may have the same gui name, if desired.
	'''
	msgDispatcher = '''
	The name that is used in the label, if the label is shown.
	Different controls can have the same gui_name, but must have a distinct unique_name.
	'''

	[classmethod.new.returns]
	what = "an ScNumericControl"
	*/

	*new {
		| unique_name, gui_name, msgDispatcher |
		^super.new.init(unique_name, gui_name, msgDispatcher);
	}

	/*
	[method.init]
	description = "Internal method to initialize the class' data members."

	[method.init.args]
	unique_name = "a unique name (string); this is an ID that must be unique over all sc-midi-controls"
	gui_name = "a name (string) that is used in the label, if that label is shown"
	msgDispatcher = "an ScMsgDispatcher, an object that handles all midi communication"
	*/
	init {
		| unique_name, gui_name, msgDispatcher |
		this.uniquename = unique_name;
		this.gui_name = gui_name;
		this.obschan = 0;
		this.obstype = nil;
		this.obsspec = nil;
		this.msg_dispatcher = msgDispatcher;
		this.receive_handler = nil;
		this.obssrc = nil;
		this.obsctrl = nil;
		this.value_lookup_table = nil;
		this.previous_value = nil;
		this.list_of_presend_coupled_controls = [];
		this.list_of_postsend_coupled_controls = [];
		this.custom_control_action = nil;
	}

	/*
	[method.extractProperties]
	description = "converts internal state of the control into a dictionary for saving/loading the learned or configured controls"

	[method.extractProperties.args]
	props = "an empty IdentityDictionary that can be filled with key/value pairs"

	[method.extractProperties.returns]
	what = "the initialized dictionary"
	*/
	extractProperties {
		| props |
		props[\obstype] = this.obstype.copy();
		props[\obssrc] = this.obssrc.copy();
		props[\obsctrl] = this.obsctrl.copy();
		props[\obschan] = this.obschan.copy();
		props[\obsspec] = this.obsspec.copy();
		props[\guiname] = this.gui_name.copy();
		props[\value_lookup_table] = this.value_lookup_table.copy();
		props[\previous_value] = this.previous_value.copy();
		props[\presendcontrols] = this.list_of_presend_coupled_controls.copy();
		props[\postsendcontrols] = this.list_of_postsend_coupled_controls.copy();
		^props;
	}

	/*
	[method.initFromProperties]
	description = "restores internal state of a control from key/values in an IdentityDictionary"

	[method.initFromProperties.args]
	props = "an initialized IdentityDictionary from which properties will be read to initialize this control"

	[method.initFromProperties.returns]
	what = "the initialized control"
	*/
	initFromProperties {
		| props |
		this.obstype = props[\obstype].copy();
		this.obssrc = props[\obssrc].copy();
		this.obsctrl = props[\obsctrl].copy();
		this.obschan = props[\obschan].copy();
		this.obsspec = props[\obsspec].copy();
		this.gui_name = props[\guiname].copy();
		this.value_lookup_table = props[\value_lookup_table].copy();
		this.previous_value = props[\previous_value].copy();
		this.list_of_presend_coupled_controls = props[\presendcontrols].copy();
		this.list_of_postsend_coupled_controls = props[\postsendcontrols].copy();
		^this;
	}

	/*
	[method.send]
	description = '''
	Method to send a controller value to a midi device.
	Actually, the send method will first send the values of all controls that are part of the list_of_presend_coupled_controls,
	then send the value of this control, and finally send the values of all controls that are part of the list_of_postsend_coupled controls. These presend and postsend controls can be used when control changes have to be sent together, e.g. when selecting a patch which first requires sending a bank select followed by a program change.
	The actual sending uses the msgDispatcher specified during creation of this control which offers sending values to the midi device as a service.
	'''

	[method.send.args]
	val = "value to send to the midi device"
	*/
	send {
		| val |
		this.list_of_presend_coupled_controls.do {
			| ctrl |
			this.msg_dispatcher.notifyControlSendPreviousValue(ctrl);
		};

		this.sendRaw(val);

		this.list_of_postsend_coupled_controls.do {
			| ctrl |
			this.msg_dispatcher.notifyControlSendPreviousValue(ctrl);
		}
	}

	/*
	[method.sendRaw]
	description = '''
	sendRaw will send this control's value to the midi device using the msgDispatcher specified during creation of this control. sendRaw is an internal function, and you are normally expected to use send instead. sendRaw will not send any presend and postsend control changes (see method send).
	'''
	[method.sendRaw.args]
	val = "value to send"
	*/
	sendRaw {
		| val |

		if (this.msg_dispatcher.midi_out.isNil) {
			"Warning: " ++ this.uniquename ++ " cannot send midi info out since its msgdispatcher midi_out member is not initialized!".postln;
		} /*else*/ {
			var chan = this.obschan;
			if (this.obsctrl.notNil) {
				switch(this.obstype)
				{ \cc } {
					this.msg_dispatcher.sendCc(this.obschan, this.obsctrl, val.asInteger);
				}
				{ \rpn } {
					this.msg_dispatcher.sendRpn(this.obschan, this.obsctrl, val.asInteger);
				}
				{ \nrpn } {
					this.msg_dispatcher.sendNrpn(this.obschan, this.obsctrl, val.asInteger);
				}
				{ \bend } {
					this.msg_dispatcher.sendBend(this.obschan, val.asInteger);
				}
				{ \prog }{
					this.msg_dispatcher.sendProgramChange(this.obschan, val.asInteger);
				};
			} /* else */ {
				"Warning: " ++ this.uniquename ++ "cannot sent control change since its obsctrl member is not initialized!".postln;
			};
		};
	}

	/*
	[method.sendPreviousValue]
	description = '''
	sendPreviousValue (re)sends the last value of this control to the midi device.
	Mostly conceived as a helper method for 'send' which calls sendPreviousValue when sending presend/postsend control changes.
	'''
	*/
	sendPreviousValue {
		if (this.previous_value.notNil) {
			this.sendRaw(this.previous_value);
		}
	}

	/*
	[method.makeLabel]
	description = '''
	Constructs a label for the control to display in the UI based on gui_name, value, control change type.
	If the control has a value_lookup_table (an array of strings) the value will be used as index in the
	value_lookup_table to show a symbolic value instead of a numerical value. If the value_lookup_table entry
	contains a percentage sign, it will be substituted with the numerical value.
	'''
	[method.makeLabel.args]
	val = "value to show in the label"
	[method.makeLabel.returns]
	what = "the constructed label (a string)"
	*/
	makeLabel {
		|val|
		if (this.obsctrl.isNil) {
			var value = "---";
			var ctrlr = "CC  ";
			var result = this.gui_name ++ "\n" ++ ctrlr ++ " " ++ "---" ++ "\nVAL " ++ value;
			^result;
		} /* else */ {
			var value_to_use = val ?? { this.previous_value ?? {"---"}};
			var value = "VAL " ++ value_to_use;
			var ctrlr;
			var result;
			if (value_to_use.notNil) {
				if (this.value_lookup_table.notNil) {
					if (this.value_lookup_table[value_to_use.asInteger].notNil) {
						value = this.value_lookup_table[value_to_use.asInteger].replace("%", value_to_use.asInteger.asString);
					};
				};
			} ;
			switch(this.obstype)
			{\cc} {
				ctrlr = "CC  " ++ " ";
			}
			{\nrpn} {
				ctrlr = "NRPN" ++ " ";
			}
			{\rpn} {
				ctrlr = "RPN " ++ " ";
			}
			{\bend} {
				ctrlr = "";
			}
			{\prog} {
				ctrlr = "";
			};
			result = this.gui_name ++ "\n"
			         ++ ctrlr ++ this.obsctrl ++ "\n"
			         ++ value;
			^result;
		};
	}

	/*
	[method.prebindCc]
	description = '''
	Convenience method to bind an sc-midi-control to a midi CC change.
	This is the programmatic way of teaching midi control what messages to listen for/what messages to send.
	When calling one of the prebind methods, the control is added to a collection of observers in the msgDispatcher
	using its unqiue name as key.
	'''
	[method.prebindCc.args]
	chan = "midi channel"
	ccnum = "controller number"
	minval = "minimum allowed value for the controller value"
	maxval = "maximum allowed value for the controller value"
	src = "midi src (can be left at nil)"
	*/
	prebindCc {
		| chan, ccnum, minval=0, maxval=127, src=nil |
		this.obstype = \cc;
		this.obssrc = src;
		this.obsctrl = ccnum;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	/*
	[method.prebindRpn]
	description = '''
	Convenience method to bind an sc-midi-control to a midi RPN change.
	This is the programmatic way of teaching midi control what messages to listen for/what messages to send.
	When calling one of the prebind methods, the control is added to a collection of observers in the msgDispatcher
	using its unqiue name as key.
	'''
	[method.prebindRpn.args]
	chan = "midi channel"
	rpnnum = "controller number"
	minval = "minimum allowed value for the controller value (default=0)"
	maxval = "maximum allowed value for the controller value (defaullt=127)"
	src = "midi src (can be left at nil)"
	*/
	prebindRpn {
		| chan, rpnnum, minval=0, maxval=127, src=nil |
		this.obstype = \rpn;
		this.obssrc = src;
		this.obsctrl = rpnnum;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	/*
	[method.prebindNrpn]
	description = '''
	Convenience method to bind an sc-midi-control to a midi NRPN change.
	This is the programmatic way of teaching midi control what messages to listen for/what messages to send.
	When calling one of the prebind methods, the control is added to a collection of observers in the msgDispatcher
	using its unqiue name as key.
	'''
	[method.prebindNrpn.args]
	chan = "midi channel"
	nrpnnum = "controller number"
	minval = "minimum allowed value for the controller value (default=0)"
	maxval = "maximum allowed value for the controller value (default=127)"
	src = "midi src (can be left at nil)"
	*/
	prebindNrpn {
		| chan, nrpnnum, minval=0, maxval=127, src=nil |
		this.obstype = \nrpn;
		this.obssrc = src;
		this.obsctrl = nrpnnum;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}


	/*
	[method.prebindBend]
	description = '''
	Convenience method to bind an sc-midi-control to a midi pitchbend change.
	This is the programmatic way of teaching midi control what messages to listen for/what messages to send.
	When calling one of the prebind methods, the control is added to a collection of observers in the msgDispatcher
	using its unqiue name as key.
	'''
	[method.prebindBend.args]
	chan = "midi channel"
	minval = "minimum allowed value for the controller value (default=0)"
	maxval = "maximum allowed value for the controller value (default=16834)"
	src = "midi src (can be left at nil)"
	*/
	prebindBend {
		| chan, minval=0, maxval=16834, src=nil |
		this.obstype = \bend;
		this.obssrc = src;
		this.obsctrl = "BEND";
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:8192, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	/*
	[method.prebindProgramChange]
	description = '''
	Convenience method to bind an sc-midi-control to a midi program change.
	This is the programmatic way of teaching midi control what messages to listen for/what messages to send.
	When calling one of the prebind methods, the control is added to a collection of observers in the msgDispatcher
	using its unqiue name as key.
	'''
	[method.prebindProgramChange.args]
	chan = "midi channel"
	minval = "minimum allowed value for the controller value (default=0)"
	maxval = "maximum allowed value for the controller value (default=127)"
	src = "midi src (can be left at nil)"
	*/
	prebindProgramChange {
		| chan, minval=0, maxval=127, src=nil |
		this.obstype = \prog;
		this.obssrc = src;
		this.obsctrl = "PROG";
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	/*
	[method.prebindLog]
	description = '''
	Convenience method to bind an sc-midi-control to an internal msg type \log.
	This is typically used in ScMidiControlChangeDumper to log midi events to a textview.
	This is the programmatic way of teaching midi control what messages to listen for/what messages to send.
	When calling one of the prebind methods, the control is added to a collection of observers in the msgDispatcher
	using its unqiue name as key.
	'''
	[method.prebindLog.args]
	chan = "midi channel"
	src = "midi src (can be left at nil)"
	*/
	prebindLog {
		| chan, src = nil |
		this.obstype = \log;
		this.obssrc = src;
		this.obsctrl = nil;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:0, maxval:16383, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	/*
	[method.registerReceiveHandler]
	description = '''
	Method that allows a control to receive a function that will be triggered
	whenever the control receives midi updates from the midi device. This can be useful to perform
	user defined actions.
	'''
	[method.registerReceiveHandler.args]
	handler = '''
	must be a function that takes 6 arguments which will be filled in by the framework, and which the user can use.
	These 6 arguments are: 1. msgDispatcher, 2. the ScMidiNumericControl itself, 3. the midi src,
	4. the midi channel, 5. the controller number (for pitch bending this is a string "BEND", for program change this
	is a string "PROG") and 6. the controller value that is received
	'''
	*/
	registerReceiveHandler {
		| handler |
		this.receive_handler = handler;
	}

	/*
	[method.registerCustomControlAction]
	description = '''
	Method that allows a custom control action to be registered. Such custom control action will be executed whenever a user modifies the state of a control (e.g. drags a slider or turns a knob).
	'''
	*/
	registerCustomControlAction {
		| handler |
		this.custom_control_action = handler;
	}

	/*
	[method.receivePublic]
	description = '''
	A notification that is triggered when a new control change is detected.
	By default, it will execute the registered receive handler (see registerReceiveHandler) if one was configured.
	The arguments of receivePublic are filled in by the framework and can be used in the custom receive handler.
	'''
	[method.receivePublic.args]
	dispatcher = "an ScMidiDispatcher"
	control = "an ScNumericControl (or one of its subclasses ScMidiKnob, ScMidiSlider, ScMidiTextView, etc)"
	src = "midi src"
	chan = "midi channel"
	num = "controller number, or one of 'BEND'/'PROG'"
	val = "controller value"
	[method.receivePublic.returns]
	what = "the result of the custom receive_handler"
	*/
	receivePublic {
		| dispatcher, control, src, chan, num, val |
		if (this.receive_handler.notNil) {
			^this.receive_handler.(dispatcher, control, src, chan, num, val);
		}
	}

	/*
	[method.receivePrivate]
	description = '''
	A notification that is triggered when a new control change is detected.
	This notification is also triggered if the control is muted. You should not override it
	as it is vital to updating the ui state whenever a midi control change is received from the midi device.
	If you want to do some custom processing when receving a control change, use the receivePublic instead, register a
	custom receive handler, or (if you insist) override the receivePublic method.
	'''
	[method.receivePrivate.args]
	dispatcher = "an ScMidiDispatcher"
	control = "an ScNumericControl (or one of its subclasses ScMidiKnob, ScMidiSlider, ScMidiTextView, etc)"
	src = "midi src"
	chan = "midi channel"
	num = "controller number, or one of 'BEND'/'PROG'"
	val = "controller value"
	*/
	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		this.previous_value = val;
		// refine in concrete controls
	}


	/*
	[method.receivePrivateSysex]
	description = "A notification triggered when sysex is received. Empty base class implementation."
	[method.receivePrivateSysex.args]
	dispatcher = "an ScMidiDispatcher"
	src = "midi src"
	data = "sysex data as Int8Array"
	*/
	receivePrivateSysex {
		| dispatcher, control, src, data |
		// implement in concrete controls
	}

	/*
	[method.refreshUI]
	description = '''
	method that is called to let controls update their labels - used when there are dependencies between controls (see the bigsky example)
	'''
	*/
	refreshUI {
		// override in concrete controls
	}
}

/*
[examples]
what = '''
(
   // ScNumericControl is normally not used by the end user.
   // It is a base class for related classes like ScMidiKnob, ScMidiSlider and ScMidiTextField.
)
'''
*/