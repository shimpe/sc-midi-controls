/*
[general]
title = "ScMidiControlChangeDumper"
summary = "a midi control change log window"
categories = "Midi Utils"
related = "Classes/ScMsgDispatcher, Classes/ScMidiTextField, Classes/ScNumericControl, Classes/ScMidiSlider, Classes/ScMidiKnob"
description = '''
ScMidiControlChangeDumper models a midi control change log window. The log window is filled up when updates are received from the midi device. ScMidiControlChangeDumper is not bidirectional: it only listens for control changes from the midi device but never sends any.
'''
*/
ScMidiControlChangeDumper : ScNumericControl {
	/*
	[method.muted]
	description='''
	On a log window, muted currently has no effect (but it could be changed in the future to stop logging).
	'''
	[method.muted.returns]
	what = "a boolean"
	*/
	var <>muted;
	/*
	[method.guiedit]
	description='''
	The TextView() instance that is displayed in the UI. Can be accessed if you want to make your own custom layouts.
	'''
	[method.guiedit.returns]
	what = "a TextView instance"
	*/
	var <>guiedit;
	/*
	[method.guimutebutton]
	description = '''
	A Button that can be used to activate the built-in mute mode for this control.
	'''
	[method.guimutebutton.returns]
	what = "a Button"
	*/
	var <>guimutebutton;
	/*
	[method.guiname]
	description = '''
	A name for this control (will be used as part of the label in the UI).
	'''
	[method.guiname.returns]
	what = "a string"
	*/
	var <>guiname;

	/*
	[method.history]
	description = '''
	history is a list of strings - this is used to only display the last 10 received control changes.
	It could be made a bit less hardcoded in the future :)
	'''
	*/
	var <>history;


	/*
	[classmethod.new]
	description = '''
	New creates a new ScMidiControlChangeDumper
	'''

	[classmethod.new.args]
	unique_name = "unique name, a string, must be unique over all bidirectional midi controls in your program"
	gui_name = "gui name, a string, needn't be unique over all bidirectional midi controls in your program - part of label"
	msgDispatcher = "an ScMsgDispatcher, the object that knows all bidirectional midi controls in your program, and that performs midi communication"
	[classmethod.new.returns]
	what = "a new ScMidiControlChangeDumper"
	*/
	*new {
		| unique_name, gui_name, msgDispatcher |
		^super.new.init(unique_name, gui_name, msgDispatcher);
	}

	/*
	[method.init]
	description = '''
	Initializes a new ScMidiControlChangeDumper
	'''

	[method.init.args]
	unique_name = "unique name, a string, must be unique over all bidirectional midi controls in your program"
	gui_name = "gui name, a string, needn't be unique over all bidirectional midi controls in your program - part of label"
	msgDispatcher = "an ScMsgDispatcher, the object that knows all bidirectional midi controls in your program, and that performs midi communication"
	[method.init.returns]
	what = "an initialized ScMidiControlChangeDumper instance"
	*/
	init {
		| unique_name, gui_name, msgDispatcher |
		super.init(unique_name, gui_name, msgDispatcher);
		this.muted = false;
		this.guiedit = TextView();
		this.guimutebutton = Button();
		this.guiname = gui_name;
	}

	/*
	[method.asLayout]
	description = '''
	Convenience method that sets up the guiknob, guilabel, guilearnbutton and guimutebutton and returns them into a VLayout
	'''
	[method.asLayout.args]
	show_mute_button = "show the mute button under the midi control (default:true)"
	mute_label = "text to display on the mute button (default: \"Mute\")"
	[method.asLayout.returns]
	what = "a VLayout containing a text view, and a button (optional)"
	*/
	asLayout {
		| show_mute_button=true, mute_label="Mute"|
		var mutebutton = this.guimutebutton.states_([
			[mute_label, Color.black, Color.gray],
			[mute_label, Color.white, Color.red]]).action_({
			|view|
			this.muted = view.value == 1;
			{this.guiknob.enabled_(this.muted.not)}.defer;
		});
		var list_of_controls = [];
		list_of_controls = list_of_controls.add(this.guiedit);
		if (show_mute_button) {
			list_of_controls = list_of_controls.add(mutebutton);
		};
		^VLayout(*list_of_controls);
	}

	/*
	[method.receivePrivate]
	description = '''
	Method that is activated every time a control value change is detected that affects this button.
	receivePrivate is also called when a control is muted so it can still update its state observing the midi device.
	'''
	[method.receivePrivate.args]
	dispatcher = "an ScMsgDispatcher, resonsible for all midi communication"
	control = "a midi control"
	src = "midi source"
	chan = "midi channel"
	num = "controller number"
	val = "controller value"
	*/
	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		var newline = "CH:" ++ chan ++ " NUM: " ++ num ++ " VAL: " ++ val;
		this.history = this.history.add(newline).keep(-10);
		{this.guiedit.string_(this.history.join("\n"));}.defer;
	}

}

/*
[examples]
what = '''
(
var slider1, slider2, knob1, textfield;
var msgDispatcher;

// create a midi msg dispatcher
// the msg dispatcher is responsible for learning and sending information to from midi device
msgDispatcher = ScMsgDispatcher();
msgDispatcher.connect("Rev2", "Rev2 MIDI 1");

// create some controls, passing in a unique id, a ui label and the midi msg dispatcher as argument
slider1 = ScMidiSlider("SLIDER 1", "slider", msgDispatcher);
// set up the slider to listen to pitch bending msgs on midi channel 0
slider1.prebindBend(0);

// create a second slider
slider2 = ScMidiSlider("SLIDER 2", "slider", msgDispatcher);
// add a custom handler that will be invoked when values are received from the midi device
slider2.registerReceiveHandler({
	| dispatcher, control, src, chan, num, val |
	src.debug(control.uniquename + "src ");
	chan.debug(control.uniquename + "chan");
	num.debug(control.uniquename + "num ");
	val.debug(control.uniquename + "val ");
});

// create a knob
knob1 = ScMidiKnob("KNOB 1", "knob", msgDispatcher);

// create a textfield
textfield = ScMidiTextField("TF", "text", msgDispatcher);

// make a window,
w = Window("Midi fader", Rect(100, 500, 400, 400));
w.layout_(HLayout(
	slider1.asLayout(show_label:false, show_mute_button:false, learn_label:"L"),
	slider2.asLayout,
	knob1.asLayout,
	textfield.asLayout(show_mute_button:false),
	nil));
w.front;

// clean up when clicking ctrl+. (or cmd+.)
CmdPeriod.doOnce({
	msgDispatcher.cleanUp;
	Window.closeAll
});

)
'''
*/