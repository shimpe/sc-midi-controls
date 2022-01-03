ScMidiTextField : ScNumericControl {
	var <>muted;
	var <>guitextfield;
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
		this.guitextfield = TextField();
		this.guilabel = StaticText();
		this.guilearnbutton = Button();
		this.guimutebutton = Button();
		this.guiname = gui_name;
	}

	asLayout {
		| show_label=true, show_learn_button=true, show_mute_button=true, learn_label="Learn", mute_label="Mute"|
		var label;
		var textfield = this.guitextfield
		.string_(
			if (this.obsspec.notNil) {
				this.obsspec.default
			} {
				0
			};
		)
		.action_({
			| view |
			var minval = if (this.obsspec.notNil) { this.obsspec.minval } { 0 };
			var maxval = if (this.obsspec.notNil) { this.obsspec.maxval } { 127 };
			var mappedvalue = view.value;
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
			{this.guitextfield.enabled_(this.muted.not)}.defer;
		});
		var list_of_controls = [];
		label = this.guilabel.string_(this.makeLabel(textfield.string));

		if (show_label) {
			list_of_controls = list_of_controls.add(label);
		};
		list_of_controls = list_of_controls.add(textfield);
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
		super.receivePrivate(dispatcher, control, src, chan, num, val);
		{this.guitextfield.string_(val.asInteger.asString)}.defer;
		{this.guilabel.string_(this.makeLabel(val))}.defer;
	}

	refreshUI {
		{this.guilabel.string_(this.makeLabel(nil))}.defer;
	}
}