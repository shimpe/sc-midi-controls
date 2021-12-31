# sc-midi-controls
Supercollider code to easily add self-learning bi-directional MIDI sliders and knobs.

To learn a new control:
 * click the "learn" button
 * move the fader or knob on the midi device over the entire range of possible values: this calibrates the range
 * then unclick the learn button

```
(
var slider1, slider2, knob1;
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

// make a window,
w = Window("Midi fader", Rect(100, 500, 400, 400));
w.layout_(HLayout(
	slider1.asLayout(show_label:false, show_mute_button:false, learn_label:"L", mute_label:"M"),
	slider2.asLayout,
	knob1.asLayout,
	nil));
w.front;

// clean up when clicking ctrl+. (or cmd+.)
CmdPeriod.doOnce({
	msgDispatcher.cleanUp;
	Window.closeAll
});

)
```
