(
s.waitForBoot({
	// first some framework code - to be converted to a quark in the future
	var objectPrototype = (
		\safeReg : {
			// adding methods to the event using safeReg will warn you if you overwrite methods
			// that already have a pre-defined meaning (like: "next").
			| self, name, implementation, verbose=0 |
			var symbolicname = name.asSymbol;
			if (self.respondsTo(symbolicname)) {
				Error("Error! Registering" + "\\" ++ symbolicname + "would overwrite a member that exists already.").throw;
			} {
				if (self.keys.includes(symbolicname)) {
					Error("Error! Cannot register" + "\\" ++ symbolicname + "twice.").throw;
				} {
					if (verbose != 0) {
						("Registered" + "\\" ++ symbolicname ++ ".").postln;
					};
					self[symbolicname] = implementation;
				};
			};
		};
	);

	var controlFactory = objectPrototype.deepCopy();
	var slider1, slider2;

	~msgDispatcher = objectPrototype.deepCopy();
	~msgDispatcher.safeReg(\debugmode, false);
	~msgDispatcher.safeReg(\connect, {
		| self, midi_device_name, midi_port_name, midi_out_latency=0 |
		self.midi_device_name = midi_device_name;
		self.midi_port_name = midi_port_name;
		CCResponder.removeAll;
		if (MIDIClient.initialized.not) { MIDIClient.init; };
		self.midi_device_out = MIDIOut.newByName(self.midi_device_name, self.midi_port_name);
		self.midi_device_out.latency_(midi_out_latency);
		self.midi_device_in = MIDIIn.connectAll;
		self.initCcResponder;
		self.initNrpnResponder;
		self.initBendResponder;
	});

	~msgDispatcher.safeReg(\observers, objectPrototype.deepCopy());
	~msgDispatcher.observers[\data] = ();
	~msgDispatcher.safeReg(\learning_observers, objectPrototype.deepCopy());
	~msgDispatcher.learning_observers[\data] = ();

	~msgDispatcher.safeReg(\learn, {
		| self, what |
		self.learning_observers[\data][what.uniquename.asSymbol] = what;
		if (self.debugmode) {
			self.learning_observers[\data].debug("learning observers");
		}
	});

	~msgDispatcher.safeReg(\stopLearning, {
		|self|
		// transfer learned observer to observers
		self.learning_observers[\data].keysValuesDo({
			| key, value |
			self.observers[\data][key.asSymbol] = value;
		});
		// remove learned observers
		self.learning_observers[\data] = ();
	});

	~msgDispatcher.safeReg(\initCcResponder, {
		| self, ignore_nrpn=true |
		if (self.debugmode) {
			"initializing CC responder".postln;
		};
		self.cc_responder = CCResponder({ |src, chan, num, val|
			if ( #[99, 98, 6, 38].includes(num) || ignore_nrpn.not) {
				// skip
			} /*else*/ {
				// if there are any controllers waiting to learn this CC, now is the time
				if (self.learning_observers[\data].size > 0) {
					self.learning_observers[\data].do {
						| learning_observer |
						learning_observer[\obstype] = \cc;
						learning_observer[\obssrc] = src;
						learning_observer[\obsctrl] = num;
						learning_observer[\obschan] = chan;
						learning_observer[\obsmidiout] = self.midi_device_out;
						if (learning_observer[\obsspec].isNil) {
							learning_observer[\obsspec] = ControlSpec(minval:0, maxval:0, step:1, default:0, units:"");
						} /*else*/ {
							// during learning, keep calibrating range
							if (val < learning_observer[\obsspec].minval) {
								if (self.debugmode) {
									("Updating minval to " ++ val).postln;
								};
								learning_observer[\obsspec].minval = val;
							};
							if (val > learning_observer[\obsspec].maxval) {
								if (self.debugmode) {
									("Updating maxval to " ++ val).postln;
								};
								learning_observer[\obsspec].maxval = val;
							};
						};
						if (self.debugmode) {
							(learning_observer.uniquename ++ "is now tied to CC" ++ num).postln;
						};
					};
				};
				// while learning already add in observers so you have live updating of the ui
				self.learning_observers[\data].keysValuesDo({
					| key, value |
					self.observers[\data][key.asSymbol] = value;
				});
			};

			// notify all relevant observers that this CC was received
			self.observers[\data].do {
				| observer |

				if (self.debugmode) {
					observer.obstype.debug("obstype");
					observer.obssrc.debug("obssrc");
					observer.obsctrl.debug("obsctrl");
					observer.obschan.debug("chan");
					observer.obsmidiout.debug("midiout");
					observer.obsspec.debug("spec");
					observer.uniquename.debug("uniquename");
				};

				if ((observer.obstype == \cc).and(
					observer.obsctrl == num).and(
					(observer.obsspec.minval <= val || observer.obsspec.minval.isNil)).and(
					(observer.obsspec.maxval >= val || observer.obsspec.maxval.isNil)).and(
					(observer.chan == chan || observer.chan.isNil)).and(
					(observer.src == src || observer.src.isNil))) {
					if (observer.keys.includes(\receive_private)) {
						if (observer.keys.includes(\muted).not.or(
							observer.keys.includes(\muted).and(observer.muted.not))) {
							observer.receive_private(src, chan, num, val);
						};
					};
					if (observer.keys.includes(\receive_handler)) {
						if (observer.keys.includes(\muted).not.or(
							observer.keys.includes(\muted).and(observer.muted.not))) {
							observer.receive_handler(src, chan, num, val);
						};
					};
				};
			};
		});
	});

	~msgDispatcher.safeReg(\initNrpnResponder, {
		| self |

		var list = #[99, 98, 6, 38];
		var seq = Pseq(list, 1).asStream;
		var incomingNum = 0;
		var incomingVal = 0;

		if (self.debugmode) {
			"initializing NRPN responder".postln;
		};

		self.nrpn_responder = CCResponder({
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

					// if there are any controllers waiting to learn this CC, now is the time
					if (self.learning_observers[\data].size > 0) {
						self.learning_observers[\data].do {
							| learning_observer |
							var old_ctrl = learning_observer[\obsctrl];
							var ctrl_change = old_ctrl != incomingNum;
							learning_observer[\obstype] = \nrpn;
							learning_observer[\obssrc] = src;
							learning_observer[\obsctrl] = incomingNum;
							learning_observer[\obschan] = chan;
							learning_observer[\obsmidiout] = self.midi_device_out;

							if (ctrl_change) {
								learning_observer[\obsspec] = ControlSpec(minval:0, maxval:0, step:1, default:0, units:"");
							} /*else*/ {
								if (incomingVal < learning_observer[\obsspec].minval) {
									if (self.debugmode) {
										("Updating minval to " ++ incomingVal).postln;
									};
									learning_observer[\obsspec].minval = val;
								};
								if (incomingVal > learning_observer[\obsspec].maxval) {
									if (self.debugmode) {
										("Updating maxval to " ++ incomingVal).postln;
									};
									learning_observer[\obsspec].maxval = incomingVal;
								};
							};

							if (self.debugmode) {
								(learning_observer.uniquename ++ "is now tied to NRPN" ++ num).postln;
							};
						};

						// transfer learned observer to observers
						self.learning_observers[\data].keysValuesDo({
							| key, value |
							self.observers[\data][key.asSymbol] = value;
						});

					};

					// notify all relevant observers that this CC was received
					self.observers[\data].do {
						| observer |

						if (self.debugmode) {
							observer.obstype.debug("obstype");
							observer.obssrc.debug("obssrc");
							observer.obsctrl.debug("obsctrl");
							observer.obschan.debug("chan");
							observer.obsmidiout.debug("midiout");
							observer.obsspec.debug("spec");
							observer.uniquename.debug("uniquename");
						};

						if ((observer.obstype == \nrpn).and(
							observer.obsctrl == incomingNum).and(
							(observer.obsspec.minval <= incomingVal || observer.obsspec.minval.isNil)).and(
							(observer.obsspec.maxval >= incomingVal|| observer.obsspec.maxval.isNil)).and(
							(observer.chan == chan || observer.chan.isNil)).and(
							(observer.src == src || observer.src.isNil))) {
							if (observer.keys.includes(\receive_private)) {
								if (observer.keys.includes(\muted).not.or(
									observer.keys.includes(\muted).and(observer.muted.not))) {
									observer.receive_private(src, chan, incomingNum, incomingVal);
								};
							};
							if (observer.keys.includes(\receive_handler)) {
								if (observer.keys.includes(\muted).not.or(
									observer.keys.includes(\muted).and(observer.muted.not))) {
									observer.receive_handler(src, chan, incomingNum, incomingVal);
								};
							};
						};
					};
					incomingNum = incomingVal = 0;
					seq.reset;
				};
			} {
				seq.reset
			};
		}, num: list);
	});

	~msgDispatcher.safeReg(\initBendResponder, {
		| self |

		if (self.debugmode) {
			"initializing BEND responder".postln;
		};

		self.bend_responder = BendResponder({ |src, chan, val|
			// if there are any controllers waiting to learn this CC, now is the time
			if (self.learning_observers[\data].size > 0) {
				self.learning_observers[\data].do {
					| learning_observer |
					learning_observer[\obstype] = \bend;
					learning_observer[\obssrc] = src;
					learning_observer[\obsctrl] = "BEND";
					learning_observer[\obschan] = chan;
					learning_observer[\obsmidiout] = self.midi_device_out;
					if (learning_observer[\obsspec].isNil) {
						learning_observer[\obsspec] = ControlSpec(minval:0, maxval:16384, step:1, default:0, units:"");
					} /*else*/ {
						// during learning, keep calibrating range
						if (val < learning_observer[\obsspec].minval) {
							if (self.debugmode) {
								("Updating minval to " ++ val).postln;
							};
							learning_observer[\obsspec].minval = val;
						};
						if (val > learning_observer[\obsspec].maxval) {
							if (self.debugmode) {
								("Updating maxval to " ++ val).postln;
							};
							learning_observer[\obsspec].maxval = val;
						};
					};
					if (self.debugmode) {
						(learning_observer.uniquename ++ "is now tied to BEND").postln;
					};
				};
			};

			// while learning already add in observers so you have live updating of the ui
			self.learning_observers[\data].keysValuesDo({
				| key, value |
				self.observers[\data][key.asSymbol] = value;
			});

			// notify all relevant observers that this CC was received
			self.observers[\data].do {
				| observer |

				if (self.debugmode) {
					observer.obstype.debug("obstype");
					observer.obssrc.debug("obssrc");
					observer.obsctrl.debug("obsctrl");
					observer.obschan.debug("chan");
					observer.obsmidiout.debug("midiout");
					observer.obsspec.debug("spec");
					observer.uniquename.debug("uniquename");
				};

				if ((observer.obstype == \bend).and(
					observer.obsctrl == "BEND").and(
					(observer.obsspec.minval <= val || observer.obsspec.minval.isNil)).and(
					(observer.obsspec.maxval >= val || observer.obsspec.maxval.isNil)).and(
					(observer.chan == chan || observer.chan.isNil)).and(
					(observer.src == src || observer.src.isNil))) {
					if (observer.keys.includes(\receive_private)) {
						if (observer.keys.includes(\muted).not.or(
							observer.keys.includes(\muted).and(observer.muted.not))) {
							observer.receive_private(src, chan, "BEND", val);
						};
					};
					if (observer.keys.includes(\receive_handler)) {
						if (observer.keys.includes(\muted).not.or(
							observer.keys.includes(\muted).and(observer.muted.not))) {
							observer.receive_handler(src, chan, "BEND", val);
						};
					};
				};
			};
		});
	});

	controlFactory.safeReg(\make_midi_slider, {
		| self, unique_name, gui_name |
		var midiSlider = objectPrototype.deepCopy();
		// learnable midi slider
		midiSlider.safeReg(\debugmode, false);
		midiSlider.safeReg(\uniquename, unique_name);
		midiSlider.safeReg(\send, {
			| self, val |
			if (self.obsmidiout.isNil) {
				if (self.debugmode) {
					"Warning: " ++ self.uniquename ++ " cannot send midi info out since its obsmidiout member is not initialized!".postln;
				};
			} /*else*/ {
				var chan = self.obschan ?? {0};
				if (self.obsctrl.notNil) {
					if (self.obstype == \cc) {
						self.obsmidiout.control(self.obschan, self.obsctrl, val);
					} /*else*/ {
						if (self.obstype == \nrpn) {
							var cCC_MSB = 99;
							var cCC_LSB = 98;
							var cDATA_MSB = 6;
							var cDATA_LSB = 38;
							var number_msb = self.obsctrl >> 7;
							var number_lsb = self.obsctrl & 127;
							var intval = val.asInteger;
							var value_msb = intval >> 7;
							var value_lsb = intval & 127;
							self.obsmidiout.control(self.obschan, cCC_MSB, number_msb);
							self.obsmidiout.control(self.obschan, cCC_LSB, number_lsb);
							self.obsmidiout.control(self.obschan, cDATA_MSB, value_msb);
							self.obsmidiout.control(self.obschan, cDATA_LSB, value_lsb);
						} {
							if (self.obstype == \bend) {
								self.obsmidiout.bend(self.obschan, val.asInteger);
							};
						};
					};
				} /* else */ {
					if (self.debugmode) {
						"Warning: " ++ self.uniquename ++ "cannot sent control change since its obsctrl member is not initialized!".postln;
					};
				};
			};
		});

		// turn into a gui thing
		midiSlider.safeReg(\orientation, \vertical);
		midiSlider.safeReg(\muted, false);
		midiSlider.safeReg(\guislider, Slider());
		midiSlider.safeReg(\guilabel, StaticText());
		midiSlider.safeReg(\guilearnbutton, Button());
		midiSlider.safeReg(\guimutebutton, Button());
		midiSlider.safeReg(\guiname, gui_name);
		midiSlider.safeReg(\makelabel, {
			|self, val|
			if (self.obsctrl.isNil) {
				var value = "---";
				var ctrlr = "CC  ";
				var result = self.guiname ++ "\n" ++ ctrlr ++ " " ++ "---" ++ "\nVAL " ++ value;
				result;
			} /* else */ {
				var value = val ?? {"---"};
				var ctrlr = if (self.obstype == \cc) { "CC  " } { if (self.obstype == \nrpn) { "NRPN" } { "CC  " } };
				var result = self.guiname ++ "\n" ++ ctrlr ++ " " ++ self.obsctrl ++ "\nVAL " ++ value;
				result;
			};
		});
		midiSlider.safeReg(\asLayout, {
			| self, name=nil, learn_label="Learn", mute_label="Mute"|
			var label;
			var slider = self.guislider
			.orientation_(self.orientation)
			.step_(if (self.obsspec.notNil) {self.obsspec.step/self.obsspec.maxval} {1.0/127};)
			.value_(
				if (self.obsspec.notNil) {
					self.obsspec.default.linlin(self.obsspec.minval, self.obsspec.maxval, 0, 1)
				} {
					0
				};
			)
			.action_({
				| view |
				var minval = if (self.obsspec.notNil) { self.obsspec.minval } { 0 };
				var maxval = if (self.obsspec.notNil) { self.obsspec.maxval } { 127 };
				if (self.muted.not) {
					var mappedvalue = view.value.linlin(0, 1, minval, maxval).asInteger;
					self.send(mappedvalue);
					{self.guilabel.string_(self.makelabel(mappedvalue))}.defer;
				};
			});
			var learnbutton = self.guilearnbutton.states_([
				[learn_label, Color.black, Color.gray],
				[learn_label, Color.white, Color.red]]).action_({
				|view|
				if (view.value == 1) {
					// switching to learning
					~msgDispatcher.learn(self);
				} /* else */ {
					if (view.value == 0) {
						~msgDispatcher.stopLearning();
					};
				};
			});
			var mutebutton = self.guimutebutton.states_([
				[mute_label, Color.black, Color.gray],
				[mute_label, Color.white, Color.red]]).action_({
				|view|
				self.muted = view.value == 1;
			});
			self.guiname = name ?? {self.guiname};

			label = self.guilabel.string_(self.makelabel(
				slider.value.linlin(
					0,
					1,
					if (self.obsspec.notNil) {self.obsspec.minval} {0},
					if (self.obsspec.notNil) {self.obsspec.maxval} {1}).asInteger));

			VLayout(label, slider, learnbutton, mutebutton);
		});
		midiSlider.safeReg(\receive_private, {
			| self, src, chan, num, val |
			var minval = if (self.obsspec.notNil) { self.obsspec.minval } { 0 };
			var maxval = if (self.obsspec.notNil) { self.obsspec.maxval } { 127 };
			{self.guislider.step_(if (self.obsspec.notNil) {self.obsspec.step/self.obsspec.maxval} {1.0/127};)}.defer;
			{self.guislider.value_(val.linlin(minval, maxval, 0, 1))}.defer;
			{self.guilabel.string_(self.makelabel(val))}.defer;
		});

		midiSlider.safeReg(\prebind_cc, {
			| self, chan, ccnum, minval=0, maxval=127, src=nil |
			self.obstype = \cc;
			self.obssrc = src;
            self.obsctrl = ccnum;
			self.obschan = chan;
			self.obsmidiout = self.midi_device_out;
			self.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
			~msgDispatcher.observers[\data][self.uniquename.asSymbol] = self;
		});
		midiSlider.safeReg(\prebind_nrpn, {
			| self, chan, nrpnnum, minval=0, maxval=127, src=nil |
			self.obstype = \nrpn;
			self.obssrc = src;
            self.obsctrl = nrpnnum;
			self.obschan = chan;
			self.obsmidiout = self.midi_device_out;
			self.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
			~msgDispatcher.observers[\data][self.uniquename.asSymbol] = self;
		});
		midiSlider.safeReg(\prebind_bend, {
			| self, chan, minval=0, maxval=16834, src=nil |
			self.obstype = \bend;
			self.obssrc = src;
			self.obsctrl = "BEND";
			self.obschan = chan;
			self.obsmidiout = self.midi_device_out;
			self.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:8192, units:"");
			~msgDispatcher.observers[\data][self.uniquename.asSymbol] = self;
		});

		midiSlider // return midiSlider
	});

	slider1 = controlFactory.make_midi_slider("SLIDER 1", "slider");
	slider1.prebind_bend(0);

	slider2 = controlFactory.make_midi_slider("SLIDER 2", "slider");
	slider2.receive_handler = {
		| self, src, chan, num, val |
		src.debug(self.uniquename + "src haha gefopt");
		chan.debug(self.uniquename + "chan haha gefopt");
		num.debug(self.uniquename + "num haha gefopt");
		val.debug(self.uniquename + "val haha gefopt");
	};

	~msgDispatcher.connect("Rev2", "Rev2 MIDI 1");

	// make a window
	w = Window("Midi fader", Rect(100, 500, 400, 400));
	w.layout_(HLayout(slider1.asLayout, slider2.asLayout, nil));
	w.front;

	CmdPeriod.doOnce({
		CCResponder.removeAll;
		Window.closeAll
	});
});
)