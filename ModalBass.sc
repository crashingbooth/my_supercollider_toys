ModalBass {
	var <>prev, <>prev2, <>note, <>scale, <>octaveSize, <>phraseLength, <>charNote, <>charNoteDict, <>root, <>dur, <>skipLastBeat, <>midiout, <>beatLength, <>legato, <>offset, <>asc, <>usedChromaticArr, <>onDeck, <>verbose, <>channel,
	<>behaviour;
	*new {|scale, root, phraseLength, midiout|
		^super.new.init(scale, root, phraseLength, midiout) }
	init { |scale, root, phraseLength = 8, midiout|
		this.charNoteDict = Dictionary.newFrom(
			List["Scale.ionian", 3,
				"Scale.dorian", 5,
				"Scale.phrygian", 1,
				"Scale.lydian", 3,
				"Scale.mixolydian", 6,
				"Scale.aeolian", 5,
				"Scale.locrian", [1,4].choose,]);
		this.setScale(scale);
		this.root = root;
		this.offset = -24;
		this.beatLength = 0.45;
		this.legato = 0.75; // \sustain = \dur * \legato
		this.phraseLength = phraseLength;
		this.midiout = midiout;
		this.onDeck = [this.scale, this.root];
 		this.verbose = false;
		this.skipLastBeat = false;
		this.usedChromaticArr = Array.fill(8, {0});
		this.behaviour = \playNormal;
		this.channel = 0;
		this.dur = 1;
		this.prev = 0;
		this.prev2 = 0;
		this.note = 0;
		this.asc = 0;
	}

	setScale { |scale, root = nil|
		this.scale = scale;
		this.charNote = this.charNoteDict[scale.asString];
		this.octaveSize = scale.size;
		if (root != nil, {this.root = root});
	}
	makeFirstBeat {
		var beatPhrase;
		if (this.prev >= (this.octaveSize - 1),
			{ this.note = this.octaveSize; this.asc = [-1,0].choose; },
			{ this.note = 0; this.asc = [1,0].choose; }
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
		this.prev2 = this.prev;
		this.prev = this.note;
		^beatPhrase;
	}

	makeSecondBeat {
		if ( (this.prev==0) && (4.rand==0),
			{ this.note = this.octaveSize;  this.asc=[-1,0].choose },
			{ this.note = this.chooseDefaultNote() } );

		this.prev2 = this.prev;
		this.prev = this.note;
		^[[this.note, this.dur]];
	}

	chooseDefaultNote {
		// returns note only, does not change globals
		var note;
		case
	// ascending
		{ this.asc == 1 }
		{ note = [this.prev+1,this.prev+2,this.prev+3,this.prev-1,this.charNote].wchoose([0.7,0.1,0.05,0.05,0.1]);}
	// descending
		{ this.asc == -1 }
		{ note = [this.prev-1,this.prev-2,this.prev-3,this.prev+1,this.charNote].wchoose([0.7,0.1,0.05,0.05,0.1]);}
	// direction not set, in lower octave
		{ (this.asc == 0) && (this.prev < this.octaveSize) }
		{ note = [this.prev+1,this.prev+2,this.prev+3,this.prev-1,this.charNote].wchoose([0.4,0.2,0.1,0.1,0.2]);}
	// direction not set, in upper octave
		{ (this.asc == 0) && (this.prev >= this.octaveSize) }
		{ note = [this.prev-1,this.prev-2,this.prev-3,this.prev+1,this.charNote].wchoose([0.4,0.2,0.1,0.1,0.2]); };

		^note;
	}

	chooseFinalNote {
		// returns note only, does not change globals
		var note, octave = (this.note/this.octaveSize).asInteger, distance = this.distanceFromTonic(), direction, chromatic = 0;

		if (distance > 0, {direction = 1 }, {direction = -1});
		if ((this.note > 0 ) && (direction < 0), {octave = octave +1 }); //hacky

		case
		{ abs(distance) > 2 }  { note = [-1,1].choose; }
		{ abs(distance) == 1 } { note = (-1 * direction) } // change direction
		{ abs(distance) == 2 } {
			if (this.shouldUseChromatic,
				{ note = 0.1 * direction; chromatic = 1  },
				{ note = -1 * direction; } )
			};
		this.updateUsedChromatic(chromatic);
		//restore original octave:
		note = note + (this.octaveSize * octave);
		^note;

	}
	shouldUseChromatic {
		// return false if just used or more than 3 of last 8
		if ((this.usedChromaticArr.sum > 3) || (this.usedChromaticArr[0] == 1),
			{ "refused chromatic".postln;^false}, {"accepted chromatic".postln;^true});

		}
	updateUsedChromatic { |used| // 1 or 0
		// keep track of 8 most recent last beat
		this.usedChromaticArr = this.usedChromaticArr.addFirst(used);
		this.usedChromaticArr = this.usedChromaticArr[0..7];
	}

	makeDefaultBeat {
		this.note = this.chooseDefaultNote();
		this.note = this.preventRootOrRepeat(this.note, this.prev, this.prev2);

		this.prev2 = this.prev;
		this.prev = this.note;
		^[[this.note, this.dur]];
	}

	makeLastBeat {
		// force a leading tone (above or below)
		if (this.skipLastBeat, {this.skipLastBeat = false;  ^[] });
		this.note = this.chooseFinalNote();

		this.prev2 = this.prev;
		this.prev = this.note;
		^[[this.note, this.dur]];
	}

	make2ndLastBeat {
		var phrase;
		if (6.rand == 0,
			{
				this.skipLastBeat = true;
				// 1st in triplet
				this.note = this.chooseDefaultNote();
				this.note = this.preventRootOrRepeat(this.note, this.prev, this.prev2);
				phrase = phrase.add([this.note, this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.note;

				// 2nd in triplet

				this.note = this.chooseDefaultNote();
				this.note = this.preventRootOrRepeat(this.note, this.prev, this.prev2);
				phrase = phrase.add([this.note, this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.note;

				// 3rd in triplet (set to upper or lower leading tone)
				this.note = this.chooseFinalNote;
				phrase = phrase.add([this.note, this.dur*2/3]);
				/*this.prev2 = this.prev;
				this.prev = this.note;*/
			}, {phrase = this.makeDefaultBeat()}
		);
		this.prev2 = this.prev;
		this.prev = this.note;
		^phrase;
	}

	distanceFromTonic {
		// called in chooseFinalNote
		var interval;
		interval = (this.scale[this.note] - this.scale[0]);

		if ( interval > 6, {interval = (interval - 12)});
		^interval;
	}

	preventRootOrRepeat { |note, prev, prev2|
		while( {((note % this.octaveSize) == 0) || (note == prev) || (note == prev2) },
			{ note = [note + 1, note - 1].choose; }
		);
		^note;

	}
	makePhrase{
		var phrase = [];

		case
		{ this.behaviour == \playNormal }  { phrase = this.normalBehaviour }
		{ this.behaviour == \playDifferent } { phrase = this.differentBehaviour };

		if (this.verbose, { this.displayPhrase(phrase) });
		^phrase;
	}

	normalBehaviour {
		var phrase = [];
		this.phraseLength.do {|i|
			var next;
			case
			{i == 0} { next = this.makeFirstBeat }
			{i == 1} { next = this.makeSecondBeat }
			{i == (this.phraseLength - 2) } { next = this.make2ndLastBeat() }
			{i == (this.phraseLength - 1) } { next = this.makeLastBeat() }
			{next = this.makeDefaultBeat }; // default case
			phrase = phrase ++ next;
		}
		^phrase;
	}

	differentBehaviour {
		var phrase = [];

		// some procedure

		^phrase;
	}
	playPhrases {
		var pb;
		pb = Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\degree, \dur], Pn(Plazy{Pseq(this.makePhrase)}),
			\chan, this.channel,
			\root, Pn(Plazy{this.root}) + this.offset,
			\scale, Pn(Plazy{this.scale}),
			\legato, Pn(Plazy{this.legato}),
			\stretch, this.beatLength,

			\amp, 0.3
		).play;
	}
	displayPhrase { |phrase|

		[this.scale, "root", this.root].postln;
		phrase.do { |beat, i|
			["deg",beat[0], "dur",beat[1]].postln;
		}
	}
}