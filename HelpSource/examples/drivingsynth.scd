(
// example to drive a supercollider synth from a midi device
s.waitForBoot ({
	var knob;
	var msgDispatcher;

	SynthDef(\mysynth, {
		| out=0, freq=440, amp=0.05 |
		var sig = amp*SinOsc.ar(freq);
		Out.ar(out, sig!2);
	}).add; // add synthdef on the server

	s.sync; // wait until the synthdef arrived on the server

	~synth = Synth(\mysynth); // start an instance of \mysynth

	// create a msgDispatcher, responsible for coordinating MIDI communication
	msgDispatcher = ScMsgDispatcher();
	msgDispatcher.connect("UMC1820", "UMC1820 MIDI 1"); // change to match your midi device
	knob = ScMidiKnob("K1", "knob", msgDispatcher);
	knob.bidirectional = false;
	knob.registerReceiveHandler({
		| msgDispatcher, theknob, src, chan, ccnum, ccval |
		var mapped_freq;
		if (ccval.notNil) {
			mapped_freq = ccval.asInteger.linexp(0, 127, 200, 1600);
			~synth.set(\freq, mapped_freq);
		}
	});
	knob.registerCustomControlAction({
		| msgDispatcher, theknob, src, chan, ccnum, ccval |
		var mapped_freq;
		if (ccval.notNil) {
			mapped_freq = ccval.asInteger.linexp(0, 127, 200, 1600);
			~synth.set(\freq, mapped_freq);
		}
	});

	w = Window("Midi knob", Rect(100, 500, 400, 400));
	w.layout_(HLayout(knob.asLayout));
	w.front;

});
)