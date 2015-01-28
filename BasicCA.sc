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
		this.windowPos = 0;
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

	setWindow { |size, pos, center = true|
		if (size > this.width, {size = this.width });
		if (center,
			{ pos = ((this.width - size)/2).asInteger});
		// make sure everything fit, else trim window
		if ((pos + size) > this.width,
			{size = this.width - pos; "trimmed window".postln; });
		this.windowPos = pos;
		this.windowSize = size;


	}

	displayCurrent {
		// called by playNext, shows complete state, with window bracketted off
		var outputString = "";
		this.nextState.do { |val, i|
			if (i == this.windowPos, {outputString = outputString ++ "["});
			outputString = outputString ++ val.asString;
			if (i == (this.windowPos + this.windowSize - 1), {outputString = outputString ++ "]"});
		};
		outputString.postln;

	}
	playNext {
		var patternFeed = [], pbs;
		this.displayCurrent();
		this.windowSize.do { |i|
			if (this.nextState[this.windowPos + i].asString != this.prevState[this.windowPos + i].asString,
				{
					switch (this.nextState[this.windowPos + i].asString,
						"0", {patternFeed = patternFeed.add([i,\noteOff])},
						"1", {patternFeed = patternFeed.add([i,\noteOn])}
					);
				}
			);
		};
		// patternFeed.do.postln;
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
		if (pbs != nil, {Ppar(pbs).play;});

		this.getNext;


	}

	playThruHelper { |tempo|
		this.playNext();
		tempo;
	}
	playThru { |tempo = 1|
		var r;
		r = Routine.new({loop {this.playNext(); tempo.yield} }).play;
	}

}
/* TODO
- create re-init, for when resetting stuff
- gen single cell, gen symetrical random (include this in sublcass args?)
*/
