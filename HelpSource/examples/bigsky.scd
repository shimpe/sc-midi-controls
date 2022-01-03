(
var logger, knobs_row1 = [], knob_layouts_row1 = [], knobs_row2 = [], knob_layouts_row2 = [], knobs_row3 = [], knob_layouts_row3 = [];
var msgDispatcher;

// create a midi msg dispatcher
// the msg dispatcher is responsible for learning and sending information to from midi device
msgDispatcher = ScMsgDispatcher();
msgDispatcher.connect("UMC1820", "UMC1820 MIDI 1");

// create a logger
logger = ScMidiControlChangeDumper("LOG 1", "LogWindow", msgDispatcher);
logger.prebindLog(nil);

// create some knobs
knobs_row1 = knobs_row1.add(ScMidiKnob("K1", "TYPE", msgDispatcher));
knobs_row1[knobs_row1.size-1].prebindCc(0, 19, 0, 11);
knobs_row1[knobs_row1.size-1].value_lookup_table = ["ROOM", "HALL", "PLATE", "SPRING", "SWELL", "BLOOM", "CLOUD", "CHORALE", "SHIMMER", "MAGNETO", "NONLIN", "REFLECT"];
knobs_row1 = knobs_row1.add(ScMidiKnob("K2", "DECAY", msgDispatcher));
knobs_row1[knobs_row1.size-1].prebindCc(0, 17, 0, 127);
knobs_row1 = knobs_row1.add(ScMidiKnob("K3", "PRE-DEL", msgDispatcher));
knobs_row1[knobs_row1.size-1].prebindCc(0, 18, 0, 127);
knobs_row1 = knobs_row1.add(ScMidiKnob("K4", "MIX", msgDispatcher));
knobs_row1[knobs_row1.size-1].prebindCc(0, 15, 0, 127);
knobs_row1 = knobs_row1.add(ScMidiKnob("K5", "TONE", msgDispatcher));
knobs_row1[knobs_row1.size-1].prebindCc(0, 3, 0, 127);
knobs_row1 = knobs_row1.add(ScMidiKnob("K6", "MOD", msgDispatcher));
knobs_row1[knobs_row1.size-1].prebindCc(0, 14, 0, 127);


knob_layouts_row1 = knobs_row1.collectAs({ |knob| knob.asLayout(show_learn_button:false, show_mute_button:false); }, HLayout);
knob_layouts_row1 = knob_layouts_row1.add(nil);

knobs_row2 = knobs_row2.add(ScMidiKnob("K7", "BOOST", msgDispatcher));
knobs_row2[knobs_row2.size-1].prebindCc(0, 23, 0, 60);
knobs_row2[knobs_row2.size-1].value_lookup_table = ["-3.0dB", "-2.9dB", "-2.8dB", "-2.7dB", "-2.6dB",
	"-2.5dB", "-2.4dB", "-2.3dB", "-2.2dB", "-2.1dB",
	"-2.0dB", "-1.9dB", "-1.8dB", "-1.7dB", "-1.6dB",
	"-1.5dB", "-1.4dB", "-1.3dB", "-1.2dB", "-1.1dB",
	"-1.0dB", "-0.9dB", "-0.8dB", "-0.7dB", "-0.6dB",
	"-0.5dB", "-0.4dB", "-0.3dB", "-0.2dB", "-0.1dB",
	"+0.0dB", "+0.1dB", "+0.2dB", "+0.3dB", "+0.4dB",
	"+0.5dB", "+0.6dB", "+0.7dB", "+0.8dB", "+0.9dB",
	"+1.0dB", "+1.1dB", "+1.2dB", "+1.3dB", "+1.4dB",
	"+1.5dB", "+1.6dB", "+1.7dB", "+1.8dB", "+1.9dB",
	"+2.0dB", "+2.1dB", "+2.2dB", "+2.3dB", "+2.4dB",
	"+2.5dB", "+2.6dB", "+2.7dB", "+2.8dB", "+2.9dB",
	"+3.0dB"
];
knobs_row2 = knobs_row2.add(ScMidiKnob("K8", "PERSIST", msgDispatcher));
knobs_row2[knobs_row2.size-1].prebindCc(0, 23, 0, 1);
knobs_row2[knobs_row2.size-1].value_lookup_table = ["OFF", "ON"];
knobs_row2 = knobs_row2.add(ScMidiKnob("K9", "FRZ/INF", msgDispatcher));
knobs_row2[knobs_row2.size-1].prebindCc(0, 70, 0, 1);
knobs_row2[knobs_row2.size-1].value_lookup_table = ["FREEZE", "INF"];
knobs_row2 = knobs_row2.add(ScMidiKnob("K10", "EXPR", msgDispatcher));
knobs_row2[knobs_row2.size-1].prebindCc(0, 70, 0, 1);
knobs_row2[knobs_row2.size-1].value_lookup_table = ["OFF", "ON"];
knobs_row2 = knobs_row2.add(ScMidiKnob("K11", "MIDI CLK", msgDispatcher));
knobs_row2[knobs_row2.size-1].prebindCc(0, 71, 0, 1);
knobs_row2[knobs_row2.size-1].value_lookup_table = ["OFF", "ON"];

knob_layouts_row2 = knobs_row2.collectAs({ |knob| knob.asLayout(show_learn_button:false, show_mute_button:false) }, HLayout);
knob_layouts_row2 = knob_layouts_row2.add(nil);

knobs_row3 = knobs_row3.add(ScMidiKnob("K12", "B1", msgDispatcher));
knobs_row3 = knobs_row3.add(ScMidiKnob("K13", "B2", msgDispatcher));
knobs_row3 = knobs_row3.add(ScMidiKnob("K14", "B3", msgDispatcher));
knobs_row3 = knobs_row3.add(ScMidiKnob("K15", "B4", msgDispatcher));
knobs_row3 = knobs_row3.add(ScMidiKnob("K16", "B5", msgDispatcher));
knobs_row3 = knobs_row3.add(ScMidiKnob("K17", "B6", msgDispatcher));

knob_layouts_row3 = knobs_row3.collectAs({ |knob| knob.asLayout(show_learn_button:false, show_mute_button:false) }, HLayout);
knob_layouts_row3 = knob_layouts_row3.add(nil);

knobs_row1[0].registerReceiveHandler({
	| dispatcher, control, src, chan, num, val |
	switch (val)
	{0} {
		// ROOM
		// ROOM - Low End 61 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 61, 0, 20);
		// ROOM - Size 59 0-1
		knobs_row3[1].gui_name = "Size";
		knobs_row3[1].prebindCc(0, 59, 0, 1);
		// ROOM - Diffusion 58 0-20
		knobs_row3[2].gui_name = "Diffusion";
		knobs_row3[2].prebindCc(0, 58, 0, 20);
		// UNUSED BUTTONS
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{1} {
		// HALL
		// HALL - Low End 39 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 39, 0, 20);
		// HALL - Size 40 0-1
		knobs_row3[1].gui_name = "Size";
		knobs_row3[1].prebindCc(0, 40, 0, 1);
		// HALL - Mid 42 0-20
		knobs_row3[2].gui_name = "Mid";
		knobs_row3[2].prebindCc(0, 42, 0, 20);
		// UNUSED BUTTONS
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{2} {
		// PLATE
		// PLATE - Low End 69 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 69, 0, 20);
		// PLATE - Size 68 0-1
		knobs_row3[1].gui_name = "Size";
		knobs_row3[1].prebindCc(0, 68, 0, 1);
		// UNUSED BUTTONS
		knobs_row3[2].gui_name = "---";
		knobs_row3[2].prebindCc(0, 128, 0, 127);
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{3} {
		// SPRING
		// SPRING - Low End 64 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 64, 0, 20);
		// SPRING - Dwell 63 0-3
		knobs_row3[1].gui_name = "Dwell";
		knobs_row3[1].prebindCc(0, 63, 0, 3);
		// SPRING - # Springs 62 0-2
		knobs_row3[2].gui_name = "#Springs";
		knobs_row3[2].prebindCc(0, 62, 0, 2);
		// UNUSED BUTTONS
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{4} {
		// SWELL
		// SWELL - Low End 65 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 65, 0, 20);
		// SWELL - Rise 66 0-22
		knobs_row3[1].gui_name = "Rise";
		knobs_row3[1].prebindCc(0, 66, 0, 22);
		// SWELL - Mode 67 0-1
		knobs_row3[2].gui_name = "Mode";
		knobs_row3[2].prebindCc(0, 67, 0, 1);
		// UNUSED BUTTONS
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{5} {
		// BLOOM
		// BLOOM - Low End 31 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 31, 0, 20);
		// BLOOM - Length 32 0-17
		knobs_row3[1].gui_name = "Length";
		knobs_row3[1].prebindCc(0, 32, 0, 17);
		// BLOOM - Feedback 30 0-17
		knobs_row3[2].gui_name = "Feedback";
		knobs_row3[2].prebindCc(0, 30, 0, 17);
		// UNUSED BUTTONS
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{6} {
		// CLOUD
		// CLOUD - Low End 38 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 38, 0, 20);
		// CLOUD - Diffusion 37 0-20
		knobs_row3[1].gui_name = "Diffusion";
		knobs_row3[1].prebindCc(0, 37, 0, 20);
		// UNUSED BUTTONS
		knobs_row3[2].gui_name = "---";
		knobs_row3[2].prebindCc(0, 128, 0, 127);
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{7} {
		// CHORALE
		// CHORALE - Vowel 33 0-6
		knobs_row3[0].gui_name = "Vowel";
		knobs_row3[0].prebindCc(0, 33, 0, 6);
		// CHORALE - Reso 34 0-2
		knobs_row3[1].gui_name = "Reso";
		knobs_row3[1].prebindCc(0, 34, 0, 2);
		// UNUSED BUTTONS
		knobs_row3[2].gui_name = "---";
		knobs_row3[2].prebindCc(0, 128, 0, 127);
		knobs_row3[3].gui_name = "---";
		knobs_row3[3].prebindCc(0, 128, 0, 127);
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{8} {
		// SHIMMER
		// SHIMMER - Shift 1 25 0-27
		knobs_row3[0].gui_name = "Shift1";
		knobs_row3[0].prebindCc(0, 25, 0, 27);
		// SHIMMER - Shift 2 26 0-28
		knobs_row3[1].gui_name = "Shift2";
		knobs_row3[1].prebindCc(0, 26, 0, 28);
		// SHIMMER - Amount 27 0-18
		knobs_row3[2].gui_name = "Amount";
		knobs_row3[2].prebindCc(0, 27, 0, 18);
		// SHIMMER - Mode 28 0-2
		knobs_row3[3].gui_name = "Mode";
		knobs_row3[3].prebindCc(0, 28, 0, 2);
		// SHIMMER - Low End 24 0-20
		knobs_row3[4].gui_name = "Low";
		knobs_row3[4].prebindCc(0, 24, 0, 20);
		// UNUSED BUTTONS
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{9} {
		// MAGNETO
		// MAGNETO - Low End 55 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 55, 0, 20);
		// MAGNETO - Diffusion 56 0-20
		knobs_row3[1].gui_name = "Diffusion";
		knobs_row3[1].prebindCc(0, 56, 0, 20);
		// MAGNETO - Heads 57 0-2
		knobs_row3[2].gui_name = "Heads";
		knobs_row3[2].prebindCc(0, 57, 0, 2);
		// MAGNETO - Spacing 54 0-1
		knobs_row3[3].gui_name = "Spacing";
		knobs_row3[3].prebindCc(0, 54, 0, 1);
		// UNUSED BUTTONS
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	}
	{10} {
		// NONLINEAR
		// NONLINEAR - Low End 44 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 44, 0, 20);
		// NONLINEAR - Shape 46 0-5
		knobs_row3[1].gui_name = "Shape";
		knobs_row3[1].prebindCc(0, 46, 0, 5);
		// NONLINEAR - Diffusion 45 0-20
		knobs_row3[2].gui_name = "Diffusion";
		knobs_row3[2].prebindCc(0, 45, 0, 20);
		// NONLINEAR - Late Decay 47 0-17
		knobs_row3[3].gui_name = "Late Dec";
		knobs_row3[3].prebindCc(0, 47, 0, 17);
		// NONLINEAR - Late Level 48 0-18
		knobs_row3[4].gui_name = "Late Lvl";
		knobs_row3[4].prebindCc(0, 48, 0, 18);
		// NONLINEAR - Mod Speed 43 0-17
		knobs_row3[5].gui_name = "Mod Speed";
		knobs_row3[5].prebindCc(0, 43, 0, 17);
	}
	{11} {
		// REFLECTIONS
		// REFLECTIONS - Low End 52 0-20
		knobs_row3[0].gui_name = "Low";
		knobs_row3[0].prebindCc(0, 52, 0, 20);
		// REFLECTIONS - Loc Y 50 0-6
		knobs_row3[1].gui_name = "Loc Y";
		knobs_row3[1].prebindCc(0, 50, 0, 6);
		// REFLECTIONS - Loc X 49 0-6
		knobs_row3[2].gui_name = "Loc X";
		knobs_row3[2].prebindCc(0, 49, 0, 6);
		// REFLECTIONS - Shape 51 0-2
		knobs_row3[3].guiname = "Shape";
		knobs_row3[3].prebindCc(0, 51, 0, 2);
		// UNUSED BUTTONS
		knobs_row3[4].gui_name = "---";
		knobs_row3[4].prebindCc(0, 128, 0, 127);
		knobs_row3[5].gui_name = "---";
		knobs_row3[5].prebindCc(0, 128, 0, 127);
	};

	msgDispatcher.refreshUI;
});


// make a window,
w = Window("Midi fader", Rect(100, 500, 800, 800));
w.layout_(VLayout(
	HLayout(*knob_layouts_row1),
	HLayout(*knob_layouts_row2),
	HLayout(*knob_layouts_row3),
	logger.asLayout));

w.front;

// clean up when clicking ctrl+. (or cmd+.)
CmdPeriod.doOnce({
	msgDispatcher.cleanUp;
	Window.closeAll
});

)

