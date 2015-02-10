ModalBass {
	var <>prev, <>note, <>scale, <>octaveSize, <>phraseLength, <>charNote, <>charNoteDict, <>root, <>dur, <>skipLastBeat, <>midiout, <>stretch, <>legato;
	*new {|scale, root, phraseLength, midiout|
		^super.new.init(scale, root, phraseLength, midiout) }
	init { |scale, root, phraseLength = 8, midiout|
		this.charNoteDict = Dictionary.newFrom(List["Scale.ionian", 5, "Scale.lydian",4,"Scale.phrygian",1,"Scale.dorian",5]);
		this.setScale(scale);
		this.root = root;
		this.stretch = 0.5;
		this.legato = 0.75; // \sustain = \dur * \legato
		this.phraseLength = phraseLength;
		this.midiout = midiout;
		this.skipLastBeat = false;
		this.dur = 1;
		this.prev = 0;
		this.note = 0;
	}

	setScale { |scale, root = nil|
		this.scale = scale;
		this.charNote = this.charNoteDict[scale.asString];
		this.octaveSize = scale.size;
		if (root != nil, {this.root = root});
	}
	decideFirstBeat {
		var beatPhrase;
		if (this.prev >= (this.octaveSize - 1),
			{ this.note = this.octaveSize },
			{ this.note = 0 }
		);

		// vary rhythm?:
		if (3.rand > 0, // 2/3 chance to vary rhythm
			{  if (5.rand > 0, // 4/5 chance of eighths
					{ beatPhrase = [[this.note, this.dur*2/3],[this.note, this.dur/3]] },
					// else triplet
					{ beatPhrase = [[this.note, this.dur/3],[this.note + [4,this.octaveSize].choose, this.dur/3],[this.note, this.dur/3]] }
				);
			},
			//else no variation
			{ beatPhrase = [[this.note, this.dur]] }
		);

		this.prev = this.note;
		^beatPhrase;
	}
	decideDefaultBeat {
		var prev = this.prev, cn = this.charNote, note;

		case
		// low notes (more than leading tone below root)
		{ prev < -2 }
		{ note=[prev+1,prev+1,prev+1,prev+1,prev+2,prev+2,prev+3,prev-1,[cn,cn,cn-this.octaveSize].choose].choose; }
		// high notes (octave or higher
		{ prev >= this.octaveSize }
		{note=[prev+1,prev+1,prev+2,prev-1,prev-1,prev-1,prev-1, prev-2,prev-2,prev-3,[cn+this.octaveSize,cn,cn].choose].choose; }
		// else
		{ note=[prev+1,prev+1,prev+1,prev+1,prev+2,prev+2,prev+3,prev-1,prev-1,prev-2,[cn,cn,cn-this.octaveSize].choose].choose; };

		note = this.preventRootOrRepeat(note, prev);

		this.note = note;
		this.prev = this.note;
		^[[this.note, this.dur]];

	}
	decideLastBeat {
		// force a leading tone (above or below)
		if (skipLastBeat, {skipLastBeat = false; "skipped".postln; ^[] });
		if (this.prev > this.octaveSize,
			{this.note = [this.octaveSize - 1, this.octaveSize + 1].choose},
			{this.note = [-1, 1].choose},
		);
		this.prev = this.note;
		^[[this.note, this.dur]];
	}
	decide2ndLastBeat {
		var phrase = [], prev = this.prev, note = this.note, cn = this.charNote;
		if (6.rand == 0,
			{
				this.skipLastBeat = true;
				// 1st in triplet
				phrase = phrase.add([this.note, this.dur*2/3]);
				this.prev = this.note;
				prev = this.prev;

				// 2nd in triplet
				if (prev<8,{
					note=[prev+1,prev+1,prev+1,prev+1,prev+2,prev+2,prev+3,
						prev-1,prev-1,prev-2,[cn,cn,cn-this.octaveSize].choose].choose;
					},{ note=[prev+1,prev+1,prev+2,prev-1,prev-1,prev-1,prev-1,
							prev-2,prev-2,prev-3,[cn+this.octaveSize,cn,cn].choose].choose;
					});
				this.note = this.preventRootOrRepeat(note, prev);
				phrase = phrase.add([this.note, this.dur*2/3]);
				this.prev = this.note;

				// 3rd in triplet (set to upper or lower leading tone)
				if (this.prev > this.octaveSize,
					{this.note = [this.octaveSize - 1, this.octaveSize + 1].choose},
					{this.note = [-1, 1].choose}
				);
				phrase = phrase.add([this.note, this.dur*2/3]);
				this.prev = this.note;
			}, {phrase = this.decideDefaultBeat()}
		);
		this.prev = this.note;
		^phrase;
	}

	preventRootOrRepeat { |note, prev|
		while( {((note % this.octaveSize) == 0) || (note == prev) },
			{ note = [note + 1, note - 1].choose; }
		);
		^note;

	}
	makePhrase{
		var phrase = [];
		this.phraseLength.do {|i|
			var next;
			case
			{i == 0} { next = this.decideFirstBeat }
			{i == (this.phraseLength - 2) } { next = this.decide2ndLastBeat() }
			{i == (this.phraseLength - 1) } { next = this.decideLastBeat() }
			{next = this.decideDefaultBeat }; // default case
			phrase = phrase ++ next;
		};
		this.displayPhrase(phrase);
		^phrase;
	}
	playPhrases {
		var pb;
		pb = Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\degree, \dur], Pn(Plazy{Pseq(this.makePhrase)}),
			\chan, 0,
			\root, Pn(Plazy{this.root}),
			\scale, Pn(Plazy{this.scale}),
			\legato, Pn(Plazy{this.legato}),
			\stretch, this.stretch,
			\amp, 0.3
		).play;
	}
	displayPhrase { |phrase|

		[this.scale, "root", this.root].postln;
		phrase.do { |beat, i|
			["deg",beat[0]+1, "dur",beat[1]].postln;
		}
	}
}