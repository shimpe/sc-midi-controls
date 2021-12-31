ScNumericControl {

	var <>uniquename;
	var <>gui_name;
	var <>obschan;
	var <>obstype;
	var <>obsspec;
	var <>obssrc;
	var <>obsctrl;

	var <>msg_dispatcher;
	var <>receive_handler;

	*new {
		| unique_name, gui_name, msg_dispatcher |
		^super.new.init(unique_name, gui_name, msg_dispatcher);
	}

	init {
		| unique_name, gui_name, msg_dispatcher |
		this.uniquename = unique_name;
		this.gui_name = gui_name;
		this.obschan = 0;
		this.obstype = nil;
		this.obsspec = nil;
		this.msg_dispatcher = msg_dispatcher;
		this.receive_handler = nil;
		this.obssrc = nil;
		this.obsctrl = nil;
	}

	send {
		| val |

		if (this.msg_dispatcher.midi_out.isNil) {
			"Warning: " ++ this.uniquename ++ " cannot send midi info out since its msgdispatcher midi_out member is not initialized!".postln;
		} /*else*/ {
			var chan = this.obschan;
			if (this.obsctrl.notNil) {
				switch(this.obstype)
				{ \cc } {
					this.msg_dispatcher.midi_out.control(this.obschan, this.obsctrl, val);
				}
				{ \nrpn } {
					var cCC_MSB = 99;
					var cCC_LSB = 98;
					var cDATA_MSB = 6;
					var cDATA_LSB = 38;
					var number_msb = this.obsctrl >> 7;
					var number_lsb = this.obsctrl & 127;
					var intval = val.asInteger;
					var value_msb = intval >> 7;
					var value_lsb = intval & 127;
					this.msg_dispatcher.midi_out.control(this.obschan, cCC_MSB, number_msb);
					this.msg_dispatcher.midi_out.control(this.obschan, cCC_LSB, number_lsb);
					this.msg_dispatcher.midi_out.control(this.obschan, cDATA_MSB, value_msb);
					this.msg_dispatcher.midi_out.control(this.obschan, cDATA_LSB, value_lsb);
				}
				{ \bend } {
					this.msg_dispatcher.midi_out.bend(this.obschan, val.asInteger);
				};
			} /* else */ {
				"Warning: " ++ this.uniquename ++ "cannot sent control change since its obsctrl member is not initialized!".postln;
			};
		};
	}

	makeLabel {
		|val|
		if (this.obsctrl.isNil) {
			var value = "---";
			var ctrlr = "CC  ";
			var result = this.gui_name ++ "\n" ++ ctrlr ++ " " ++ "---" ++ "\nVAL " ++ value;
			^result;
		} /* else */ {
			var value = val ?? {"---"};
			var ctrlr = if (this.obstype == \cc) { "CC  " } { if (this.obstype == \nrpn) { "NRPN" } { "CC  " } };
			var result = this.gui_name ++ "\n" ++ ctrlr ++ " " ++ this.obsctrl ++ "\nVAL " ++ value;
			^result;
		};
	}

	prebindCc {
		| chan, ccnum, minval=0, maxval=127, src=nil |
		this.obstype = \cc;
		this.obssrc = src;
		this.obsctrl = ccnum;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	prebindNrpn {
		| chan, nrpnnum, minval=0, maxval=127, src=nil |
		this.obstype = \nrpn;
		this.obssrc = src;
		this.obsctrl = nrpnnum;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	prebindBend {
		| chan, minval=0, maxval=16834, src=nil |
		this.obstype = \bend;
		this.obssrc = src;
		this.obsctrl = "BEND";
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:8192, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	registerReceiveHandler {
		| handler |
		// handler must be a function that has 6 arguments
		// dispatcher (filled in by the framework)
		// control (the knob or slider that received the update)
		// src (the midi source)
		// chan (the midi channel)
		// num (the controller number (for pitch bending this is a string "BEND")
		// val (the controller value)
		this.receive_handler = handler;
	}

	receivePublic {
		| dispatcher, control, src, chan, num, val |
		if (this.receive_handler.notNil) {
			^this.receive_handler.(dispatcher, control, src, chan, num, val);
		}
	}

	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		// override in concrete controls
	}

}