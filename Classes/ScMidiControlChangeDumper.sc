ScMidiControlChangeDumper : ScNumericControl {
	var <>muted;
	var <>guiedit;
	var <>guimutebutton;
	var <>guiname;

	var <>history;

	*new {
		| unique_name, gui_name, msgDispatcher |
		^super.new.init(unique_name, gui_name, msgDispatcher);
	}

	init {
		| unique_name, gui_name, msgDispatcher |
		super.init(unique_name, gui_name, msgDispatcher);
		this.muted = false;
		this.guiedit = TextView();
		this.guimutebutton = Button();
		this.guiname = gui_name;
	}

	asLayout {
		| show_mute_button=true, mute_label="Mute"|
		var mutebutton = this.guimutebutton.states_([
			[mute_label, Color.black, Color.gray],
			[mute_label, Color.white, Color.red]]).action_({
			|view|
			this.muted = view.value == 1;
			{this.guiknob.enabled_(this.muted.not)}.defer;
		});
		var list_of_controls = [];
		list_of_controls = list_of_controls.add(this.guiedit);
		if (show_mute_button) {
			list_of_controls = list_of_controls.add(mutebutton);
		};
		^VLayout(*list_of_controls);
	}

	receivePrivate {
		| dispatcher, control, src, chan, num, val |
		var newline = "CH:" ++ chan ++ " NUM: " ++ num ++ " VAL: " ++ val;
		this.history = this.history.add(newline).keep(-10);
		{this.guiedit.string_(this.history.join("\n"));}.defer;
	}

}