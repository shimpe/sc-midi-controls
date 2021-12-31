# sc-midi-controls
supercollider code to easily add self-learning bi-directional MIDI sliders and knobs

```
(
var slider1, slider2, knob1;
var msgDispatcher;

// create a midi msg dispatcher
msgDispatcher = ScMsgDispatcher();
msgDispatcher.connect("Rev2", "Rev2 MIDI 1");

// create some controls, passing in the midi msg dispatcher as argument
slider1 = ScMidiSlider("SLIDER 1", "slider", msgDispatcher);
slider1.prebindBend(0);

slider2 = ScMidiSlider("SLIDER 2", "slider", msgDispatcher);
slider2.registerReceiveHandler({
	| dispatcher, control, src, chan, num, val |
	src.debug(control.uniquename + "src ");
	chan.debug(control.uniquename + "chan");
	num.debug(control.uniquename + "num ");
	val.debug(control.uniquename + "val ");
});

knob1 = ScMidiKnob("KNOB 1", "knob", msgDispatcher);


// make a window,
w = Window("Midi fader", Rect(100, 500, 400, 400));
w.layout_(HLayout(
	slider1.asLayout(show_label:false, show_mute_button:false, learn_label:"L", mute_label:"M"),
	slider2.asLayout,
	knob1.asLayout,
	nil));
w.front;

CmdPeriod.doOnce({
	msgDispatcher.cleanUp;
	Window.closeAll
});

)
```

