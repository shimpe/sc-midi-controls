/*
[general]
title = "ScMidiSlider"
summary = "a bidirectional midi slider"
categories = "Midi Utils"
related = "Classes/ScMsgDispatcher, Classes/ScMidiTextField, Classes/ScNumericControl, Classes/ScMidiKnob, Classes/ScMidiControlChangeDumper"
description = '''
ScMidiSlider models a bidirectional MIDI slider. The slider updates when updates are received from the midi device. If the slider is modified in the UI, the new values are sent to the midi device.
'''
*/
ScMidiSlider : ScNumericControl {
	/*
	[method.muted]
	description='''
	A boolean to indicate that the slider is muted. When a slider is muted,
	it doesn't send values to the midi device when the user changes the slider.
	The slider will still update its displayed value if control changes are received from the midi device though.
	'''
	[method.muted.returns]
	what = "a boolean"
	*/
	var <>muted;
	/*
	[method.guislider]
	description='''
	The Slider() instance that is displayed in the UI. Can be accessed if you want to make your own custom layouts.
	'''
	[method.guislider.returns]
	what = "a Slider instance"
	*/
	var <>guislider;
	/*
	[method.guilabel]
	description = '''
	A StaticText instance that is used to display the label.
	'''
	[method.guilabel.returns]
	what = "a StaticText instance"
	*/
	var <>guilabel;
	/*
	[method.guilearnbutton]
	description = '''
	A Button that can be used to activate the built-in midi learning mode for this control.
	'''
	[method.guilearnbutton.returns]
	what = "a Button"
	*/
	var <>guilearnbutton;
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
	[classmethod.new]
	description = '''
	New creates a new ScMidiSlider
	'''
	[classmethod.new.args]
	unique_name = "unique name, a string, must be unique over all bidirectional midi controls in your program"
	gui_name = "gui name, a string, needn't be unique over all bidirectional midi controls in your program - part of label"
	msgDispatcher = "an ScMsgDispatcher, the object that knows all bidirectional midi controls in your program, and that performs midi communication"
	[classmethod.new.returns]
	what = "a new ScMidiSlider"
	*/
	*new {
		| unique_name, gui_name, msgDispatcher |
		^super.new.init(unique_name, gui_name, msgDispatcher);
	}

	/*
	[method.init]
	description = '''
	Initializes a new ScMidiSlider
	'''

	[method.init.args]
	unique_name = "unique name, a string, must be unique over all bidirectional midi controls in your program"
	gui_name = "gui name, a string, needn't be unique over all bidirectional midi controls in your program - part of label"
	msgDispatcher = "an ScMsgDispatcher, the object that knows all bidirectional midi controls in your program, and that performs midi communication"
	[method.init.returns]
	what = "an initialized ScMidiSlider instance"
	*/
	init {
		| unique_name, gui_name, msgDispatcher |
		super.init(unique_name, gui_name, msgDispatcher);
		this.muted = false;
		this.guislider = Slider();
		this.guilabel = StaticText();
		this.guilearnbutton = Button();
		this.guimutebutton = Button();
		this.guiname = gui_name;
	}

	/*
	[method.asLayout]
	description = '''
	Convenience method that sets up the guislider, guilabel, guilearnbutton and guimutebutton and returns them into a VLayout.
	'''
	[method.asLayout.args]
	show_label = "show the label above the midi control (default: true)"
	show_learn_button = "show the learn button under the midi control (default:true)"
	show_mute_button = "show the mute button under the midi control (default:true)"
	learn_label = "text to display on the learn button (default: \"Learn\")"
	mute_label = "text to display on the mute button (default: \"Mute\")"
	[method.asLayout.returns]
	what = "a VLayout containing a label (optional), a slider, and two buttons (optional)"
	*/
	asLayout {
		| show_label=true, show_learn_button=true, show_mute_button=true, learn_label="Learn", mute_label="Mute"|
		var label;
		var slider = this.guislider
		.orientation_(\vertical)
		.step_(if (this.obsspec.notNil) {this.obsspec.step/this.obsspec.maxval} {1.0/127};)
		.value_(
			if (this.obsspec.notNil) {
				this.obsspec.default.linlin(this.obsspec.minval, this.obsspec.maxval, 0, 1)
			} {
				0
			};
		)
		.action_({
			| view |
			var minval = if (this.obsspec.notNil) { this.obsspec.minval } { 0 };
			var maxval = if (this.obsspec.notNil) { this.obsspec.maxval } { 127 };
			var mappedvalue = view.value.linlin(0, 1, minval, maxval).asInteger;
			this.previous_value = mappedvalue;
			{this.guilabel.string_(this.makeLabel(mappedvalue))}.defer;
			if (this.muted.not) {
				this.send(mappedvalue);
			};
			if (this.custom_control_action.notNil) {
				custom_control_action.(view);
			};
		});
		var learnbutton = this.guilearnbutton.states_([
			[learn_label, Color.black, Color.gray],
			[learn_label, Color.white, Color.red]]).action_({
			|view|
			if (view.value == 1) {
				// switching to learning
				this.msg_dispatcher.learn(this);
			} /* else */ {
				if (view.value == 0) {
					this.msg_dispatcher.stopLearning();
				};
			};
		});
		var mutebutton = this.guimutebutton.states_([
			[mute_label, Color.black, Color.gray],
			[mute_label, Color.white, Color.red]]).action_({
			|view|
			this.muted = view.value == 1;
			{this.guislider.enabled_(this.muted.not)}.defer;
		});
		var list_of_controls = [];
		label = this.guilabel.string_(this.makeLabel(
			slider.value.linlin(
				0,
				1,
				if (this.obsspec.notNil) {this.obsspec.minval} {0},
				if (this.obsspec.notNil) {this.obsspec.maxval} {1}).asInteger));

		if (show_label) {
			list_of_controls = list_of_controls.add(label);
		};
		list_of_controls = list_of_controls.add(slider);
		if (show_learn_button) {
			list_of_controls = list_of_controls.add(learnbutton);
		};
		if (show_mute_button) {
			list_of_controls = list_of_controls.add(mutebutton);
		};

		^VLayout(*list_of_controls);
	}

	/*
	[method.receivePrivate]
	description = '''
	Method that is activated every time a control value change is detected that affects this button.
	receivePrivate is also called when a control is muted so it can still update its state observing the midi device
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
		var minval = if (this.obsspec.notNil) { this.obsspec.minval } { 0 };
		var maxval = if (this.obsspec.notNil) { this.obsspec.maxval } { 127 };
		super.receivePrivate(dispatcher, control, src, chan, num, val);
		{this.guislider.step_(if (this.obsspec.notNil) {this.obsspec.step/this.obsspec.maxval} {1.0/127};)}.defer;
		{this.guislider.value_(val.linlin(minval, maxval, 0, 1))}.defer;
		{this.guilabel.string_(this.makeLabel(val))}.defer;
	}

	/*
	[method.refreshUI]
	description = '''
	method called when all controls in the system need to reevaluate their labels, e.g. because of dependencies between controls
	'''
	*/
	refreshUI {
		{this.guilabel.string_(this.makeLabel(nil))}.defer;
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