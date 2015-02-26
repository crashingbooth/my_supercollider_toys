ModalBass {
	var <>prev, <>prev2, <>note, <>scale, <>octaveSize, <>phraseLength, <>charNote, <>charNoteDict, <>root, <>dur, <>skipLastBeat, <>midiout, <>tempoclock,<>legato, <>offset, <>asc, <>usedChromaticArr, <>onDeck, <>verbose, <>channel,
	<>behaviour, <>beatCounter;
	*new {|scale, root, phraseLength, midiout, tempoclock, conductor|
		^super.new.init(scale, root, phraseLength, midiout, tempoclock) }
	init { |scale, root, phraseLength = 8, midiout, tempoclock|
		this.charNoteDict = Dictionary.newFrom(
			List["Scale.ionian", 3,
				"Scale.dorian", 5,
				"Scale.phrygian", 1,
				"Scale.lydian", 3,
				"Scale.mixolydian", 6,
				"Scale.aeolian", 5,
				"Scale.locrian", [1,4].choose,]);
		// this.note = 0;
		this.root = root;
		this.setScale(scale);
		this.offset = 36;
		if (tempoclock == nil,
			{ this.tempoclock = TempoClock.new(132/60)}, { this.tempoclock = tempoclock });
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
		this.asc = 0;
		this.note = 0;
		this.beatCounter = 0;

	}

	setScale { |scale, root = nil|
		var realPitch, finalPitch;
		// get absolute note value
		if (this.note != nil, {
			realPitch =  ModalBass.getRealPitch(this.note, this.scale, this.root);});
		this.scale = scale;
		this.charNote = this.charNoteDict[scale.asString];
		this.octaveSize = scale.size;

		if (root != nil, {this.root = root});
		["scale changed to", this.scale, this.root].postln;
		if (this.note != nil, {
			this.note = ModalBass.getDegreeFromPitch(realPitch, this.scale, this.root);});

	}

	*getRealPitch { |note, scale, root|
		var res, semitone = 0;
		// if not scale tone, adjust here:
		if ((note % 1) > 0 , {
			semitone = -1;
			note = note + 0.5;
		});

		//performDegreeToKey is broken for *some* non-scale tones!!
		res = (scale.performDegreeToKey(note) + root + semitone);
		^res;
	}
	*getDegreeFromPitch {|note, scale, root|
		var res, diff;
		res = scale.performKeyToDegree(note - root);
		diff = (note - ModalBass.getRealPitch(res, scale, root));

		^(res + diff);
	}


	prepareNextMode { |scale, root|
		this.onDeck = [scale, root];
		// ["onDeck set to", scale, root].postln;
	}
	makeFirstBeat {
		var beatPhrase, prevLast;
		if (this.prev >= (this.octaveSize - 1),
			{ this.note = this.octaveSize; this.asc = [-1,0].choose; },
			{ this.note = 0; this.asc = [1,0].choose; }
		);

		// vary rhythm?:
		if (3.rand > 0, // 2/3 chance to vary rhythm
			{  if (5.rand > 0, // 4/5 chance of eighths
				{ beatPhrase = [[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur*2/3],
					[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur/3]] },
					// else triplet
					{ beatPhrase = [[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur/3],
						[ModalBass.getRealPitch(this.note + [4,this.octaveSize].choose, this.scale, this.root), this.dur/3],
						[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur/3]] }
				);
			},
			//else no variation
			{ beatPhrase = [[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur]] }
		);
		this.prev2 = this.prev;
		if (this.prev == this.note, {"REPEATED NOTE!"});
		this.prev = this.note;

		^beatPhrase;
	}

	makeSecondBeat {
		if ( (this.prev==0) && (4.rand==0),
			{ this.note = this.octaveSize;  this.asc=[-1,0].choose },
			{ this.note = this.chooseDefaultNote() } );

		this.prev2 = this.prev;
		this.prev = this.note;
		^[[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur]];
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
		var note, octave = (this.note/this.octaveSize).asInteger, distance, direction, chromatic = 0, debug;

		// change tonic early if mode/root is changing next phrase
		if (this.onDeck != [this.scale, this.root], {this.setScale(this.onDeck[0], this.onDeck[1]);});

		distance = this.distanceFromTonic(this.note);

		if (distance > 0, {direction = 1 }, {direction = -1});
		if ((this.note > 0 ) && (direction < 0), {octave = octave +1 }); //hacky

		case
		{ abs(distance) > 2 }  { note = [-1,1].choose; debug = 0}
		{ abs(distance) == 1 } { note = (-1 * direction); debug =1 } // change direction
		{ abs(distance) == 0.5 } {note = (-1 * direction); debug =2 }
		{ abs(distance) == 0 } { note = [-1,1].choose; debug =3}
		{ abs(distance) == 2 } {
			if (this.shouldUseChromatic,
				{ note = 0.5 * direction; chromatic = 1; debug = 4; },
				{ note = (-1 * direction);debug = 5; } );
			};


		this.updateUsedChromatic(chromatic);
		//restore original octave:
		note = note + (this.octaveSize * octave);
		// ["Took Path", debug].postln;

		^note;

	}
	shouldUseChromatic {
		// return false if just used or more than 3 of last 8
		if ((this.usedChromaticArr.sum > 3) || (this.usedChromaticArr[0] == 1),
			{ ^false}, { ^true});

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
		^[[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur]];
	}

	makeLastBeat {
		// force a leading tone (above or below)
		if (this.skipLastBeat, {this.skipLastBeat = false;  ^[0,0] });
		this.note = this.chooseFinalNote();

		this.prev2 = this.prev;
		this.prev = this.note;
		^[[ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur]];
	}

	make2ndLastBeat {
		var phrase;
		if (6.rand == 0,
			{
				this.skipLastBeat = true;
				// 1st in triplet
				this.note = this.chooseDefaultNote();
				this.note = this.preventRootOrRepeat(this.note, this.prev, this.prev2);
				phrase = phrase.add([ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.note;

				// 2nd in triplet

				this.note = this.chooseDefaultNote();
				this.note = this.preventRootOrRepeat(this.note, this.prev, this.prev2);
				phrase = phrase.add([ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.note;

				// 3rd in triplet (set to upper or lower leading tone)
				this.note = this.chooseFinalNote;
				phrase = phrase.add([ModalBass.getRealPitch(this.note, this.scale, this.root), this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.note;
			}, {phrase = this.makeDefaultBeat()}
		);
		this.prev2 = this.prev;
		this.prev = this.note;
		^phrase;
	}

	distanceFromTonic { |note|
		// called in chooseFinalNote
		var interval, releventTonic, semitone = 0;
		if ((note % 1) > 0, {semitone = 1});
		interval = (this.scale[note] - this.scale[0]);

		if ( interval > 6, {interval = (interval - 12 - semitone)},
			{interval = interval + semitone});
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
		var phrase = [], next;
		this.beatCounter = this.beatCounter % this.phraseLength;
		case
			{this.beatCounter == 0} { next = this.makeFirstBeat }
			{this.beatCounter == 1} { next = this.makeSecondBeat }
			{this.beatCounter == (this.phraseLength - 2) } { next = this.make2ndLastBeat() }
			{this.beatCounter == (this.phraseLength - 1) } { next = this.makeLastBeat() }
			{next = this.makeDefaultBeat }; // default case

		if (this.skipLastBeat, {this.skipLastBeat = false; this.beatCounter = this.beatCounter + 1; });
		this.beatCounter = this.beatCounter + 1;
		^next
	}
	playScore {
	}

	differentBehaviour {
		var phrase = [];

		// some procedure

		^phrase;
	}
	play {
		var pb;
		pb = Pbind (
			\type, \midi,
			\midiout, this.midiout,
			[\temp, \dur], Pn(Plazy{Pseq(this.makePhrase)}),
			\midinote, Pkey(\temp) + this.offset,
			\chan, this.channel,
			\legato, Pn(Plazy{this.legato}),
			\amp, 0.3
		).play(this.tempoclock);
		^pb;
	}
	displayPhrase { |phrase|

		[this.scale, "root", this.root].postln;
		phrase.do { |beat, i|
			["deg",ModalBass.getNoteName(beat[0]), "dur",beat[1]].postln;
		}
	}

	*testConversion {
		var scaleList = [Scale.ionian, Scale.dorian, Scale.phrygian, Scale.lydian, Scale.locrian, Scale.mixolydian, Scale.aeolian, Scale.locrian], inScale = scaleList.choose, outScale = scaleList.choose, inRoot = rrand(-12,12), outRoot = rrand(-12,12), startNote = rrand(-5, 12), rawPitch, outNote, testedPitch;
		rawPitch = ModalBass.getRealPitch(startNote, inScale, inRoot);
		["inscale", inScale, "inRoot", inRoot, ModalBass.getNoteName(inRoot),"note", startNote, "raw", rawPitch, ModalBass.getNoteName(rawPitch)].postln;
		outNote = ModalBass.getDegreeFromPitch(rawPitch, outScale, outRoot);
		["ouscale", outScale, "outRoot", outRoot, ModalBass.getNoteName(outRoot),"outnote", outNote, "raw", rawPitch, ModalBass.getNoteName(inRoot)].postln;
		testedPitch = ModalBass.getRealPitch(outNote, outScale, outRoot);
		[testedPitch == rawPitch, "raw", rawPitch, ModalBass.getNoteName(inRoot),"result", testedPitch, ModalBass.getNoteName(rawPitch),ModalBass.getNoteName(testedPitch)].postln;

	}
	*getNoteName { |midiNote|
		var pitchClass = ["C", "C#/Db", "D", "Eb", "E","F", "F#/Gb", "G", "G#/Ab","A","A#/Bb","B"], octave;
		octave = (midiNote/12).asInteger;
		if (midiNote < 0, {octave = octave -1});
		^(pitchClass.wrapAt(midiNote) ++ octave.asString);

	}
}