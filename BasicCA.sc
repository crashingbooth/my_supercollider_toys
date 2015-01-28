BasicCA {
	/* abstract class for two state, 1D Cellular Automata.
	Subclasses will override methods related to rule creation and derivation.
	This superclass will handle conversion to patterns
	*/

	var <>width, <>rules, <>prevState, <>nextState, <>started, <>history, <>midiout, <>windowSize, <>windowPos;

	*new {|width, rules, firstState, midiout|
		^super.new.init(width, rules, firstState, midiout)
	}

	init {|width, rules, firstState, midiout|
		this.width = width;
		this.midiout = midiout;
		if (firstState == nil,
			{ this.nextState = this.randomGenerate() },
			{ this.nextState = firstState; this.width = this.nextState.size()}
		);
		this.prevState = "";
		this.nextState.do { this.prevState = this.prevState ++ "0" };
		this.started = false;
		this.history = [this.prevState];
		this.windowSize = this.width;
		// this.rules = this.createRules(rules);
	}

	createRules {
		// abstract
	}

	getNext {
		// abstract
	}

	displayHistory {
		this.history.do {|state, i|
			if (i >0 , {state.postln});
		};
	}

	randomGenerate {
		var res ="";
		this.width.do { res = res ++ 2.rand.asString };
		^res;
	}
	playNext {
		var patternFeed = [], pbs;
		this.nextState.do { |val, i|
			if (val.asString != this.prevState[i].asString,
				{
					switch (val.asString,
						"0", {patternFeed = patternFeed.add([i,\noteOff])},
						"1", {patternFeed = patternFeed.add([i,\noteOn])}
					);
				}
			);
		};
		patternFeed.do.postln;
		patternFeed.do {|feed, i|
			var pb;
			pb = Pbind (
				\type, \midi,
				\midiout, this.midiout,
				\midicmd, feed[1],
				\degree, Pseq([feed[0]]),
				\chan, 0,
				\root, 0,
				\dur, 100,
				\scale, Scale.iwato,
			);
			pbs = pbs.add(pb);
		};
		Ppar(pbs).play;
		this.getNext;


	}

}