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
	var <>value_lookup_table;

	var <>previous_value;

	var <>list_of_presend_coupled_controls;
	var <>list_of_postsend_coupled_controls;
	var <>custom_control_action;

	*new {
		| unique_name, gui_name, msgDispatcher |
		^super.new.init(unique_name, gui_name, msgDispatcher);
	}

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

	sendPreviousValue {
		if (this.previous_value.notNil) {
			this.sendRaw(this.previous_value);
		}
	}

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

	prebindCc {
		| chan, ccnum, minval=0, maxval=127, src=nil |
		this.obstype = \cc;
		this.obssrc = src;
		this.obsctrl = ccnum;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	prebindRpn {
		| chan, rpnnum, minval=0, maxval=127, src=nil |
		this.obstype = \rpn;
		this.obssrc = src;
		this.obsctrl = rpnnum;
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

	prebindProgramChange {
		| chan, minval=0, maxval=127, src=nil |
		this.obstype = \prog;
		this.obssrc = src;
		this.obsctrl = "PROG";
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:minval, maxval:maxval, step:1, default:0, units:"");
		this.msg_dispatcher.observers[this.uniquename.asSymbol] = this;
	}

	prebindLog {
		| chan, src = nil |
		this.obstype = \log;
		this.obssrc = src;
		this.obsctrl = nil;
		this.obschan = chan;
		this.obsspec = ControlSpec(minval:0, maxval:16383, step:1, default:0, units:"");
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

	registerCustomControlAction {
		| handler |
		this.custom_control_action = handler;
	}

	receivePublic {
		| dispatcher, control, src, chan, num, val |
		if (this.receive_handler.notNil) {
			^this.receive_handler.(dispatcher, control, src, chan, num, val);
		}
	}

	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		this.previous_value = val;
		// refine in concrete controls
	}

	refreshUI {
		// override in concrete controls
	}
}
