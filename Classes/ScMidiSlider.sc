ScMidiSlider : ScNumericControl {
	var <>muted;
	var <>guislider;
	var <>guilabel;
	var <>guilearnbutton;
	var <>guimutebutton;
	var <>guiname;

	*new {
		| unique_name, gui_name, msg_dispatcher |
		^super.new.init(unique_name, gui_name, msg_dispatcher);
	}

	init {
		| unique_name, gui_name, msg_dispatcher |
		super.init(unique_name, gui_name, msg_dispatcher);
		this.muted = false;
		this.guislider = Slider();
		this.guilabel = StaticText();
		this.guilearnbutton = Button();
		this.guimutebutton = Button();
		this.guiname = gui_name;
	}

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
			if (this.muted.not) {
				var mappedvalue = view.value.linlin(0, 1, minval, maxval).asInteger;
				this.send(mappedvalue);
				{this.guilabel.string_(this.makeLabel(mappedvalue))}.defer;
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

	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		var minval = if (this.obsspec.notNil) { this.obsspec.minval } { 0 };
		var maxval = if (this.obsspec.notNil) { this.obsspec.maxval } { 127 };
		{this.guislider.step_(if (this.obsspec.notNil) {this.obsspec.step/this.obsspec.maxval} {1.0/127};)}.defer;
		{this.guislider.value_(val.linlin(minval, maxval, 0, 1))}.defer;
		{this.guilabel.string_(this.makeLabel(val))}.defer;
	}
}