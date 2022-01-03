ScMidiKnob : ScNumericControl {
	var <>muted;
	var <>guiknob;
	var <>guilabel;
	var <>guilearnbutton;
	var <>guimutebutton;
	var <>guiname;

	*new {
		| unique_name, gui_name, msgDispatcher |
		^super.new.init(unique_name, gui_name, msgDispatcher);
	}

	init {
		| unique_name, gui_name, msgDispatcher |
		super.init(unique_name, gui_name, msgDispatcher);
		this.muted = false;
		this.guiknob = Knob();
		this.guilabel = StaticText();
		this.guilearnbutton = Button();
		this.guimutebutton = Button();
		this.guiname = gui_name;
	}

	asLayout {
		| show_label=true, show_learn_button=true, show_mute_button=true, learn_label="Learn", mute_label="Mute"|
		var label;
		var knob = this.guiknob
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
			{this.guilabel.string_(this.makeLabel(mappedvalue))}.defer;
			if (this.muted.not) {
				this.send(mappedvalue);
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
			{this.guiknob.enabled_(this.muted.not)}.defer;
		});
		var list_of_controls = [];
		label = this.guilabel.string_(this.makeLabel(
			knob.value.linlin(
				0,
				1,
				if (this.obsspec.notNil) {this.obsspec.minval} {0},
				if (this.obsspec.notNil) {this.obsspec.maxval} {1}).asInteger));

		if (show_label) {
			list_of_controls = list_of_controls.add(label);
		};
		list_of_controls = list_of_controls.add(knob);
		if (show_learn_button) {
			list_of_controls = list_of_controls.add(learnbutton);
		};
		if (show_mute_button) {
			list_of_controls = list_of_controls.add(mutebutton);
		};

		^VLayout(*list_of_controls);
	}

	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		var minval = if (this.obsspec.notNil) { this.obsspec.minval } { 0 };
		var maxval = if (this.obsspec.notNil) { this.obsspec.maxval } { 127 };
		{this.guiknob.step_(if (this.obsspec.notNil) {this.obsspec.step/this.obsspec.maxval} {1.0/127};)}.defer;
		{this.guiknob.value_(val.linlin(minval, maxval, 0, 1))}.defer;
		{this.guilabel.string_(this.makeLabel(val))}.defer;
	}

	refreshUI {
		{this.guilabel.string_(this.makeLabel(nil))}.defer;
	}

}