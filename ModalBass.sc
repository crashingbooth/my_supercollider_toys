ModalBass {
	var <>prev, <>prev2, <>degree, <>scale, <>octaveSize, <>phraseLength, <>charNote, <>charNoteDict, <>root, <>dur, <>skipLastBeat, <>midiout, <>tempoclock,<>legato, <>offset, <>asc, <>usedChromaticArr, <>onDeck, <>verbose, <>channel, <>changeToScore,
	<>behaviour, <>beatCounter, <>scoreBag;
	*new {|scale, root, phraseLength, midiout, tempoclock|
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
		// this.degree = 0;
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
		this.degree = 0;
		this.beatCounter = 0;
		this.scoreBag = nil;
		this.changeToScore = False;

	}

	setScale { |scale, root = nil|
		var realPitch, finalPitch;
		// get absolute note value
		if (this.degree != nil, {
			realPitch =  ModalBass.getRealPitch(this.degree, this.scale, this.root);});
		this.scale = scale;
		this.charNote = this.charNoteDict[scale.asString];
		this.octaveSize = scale.size;

		if (root != nil, {this.root = root});
		["scale changed to", this.scale, this.root].postln;
		if (this.degree != nil, {
			this.degree = ModalBass.getDegreeFromPitch(realPitch, this.scale, this.root);});

	}

	*getRealPitch { |degree, scale, root|
		var res, semitone = 0;
		// if not scale tone, adjust here:
		if ((degree % 1) > 0 , {
			semitone = -1;
			degree = degree + 0.5;
		});

		//performDegreeToKey is broken for *some* non-scale tones!!
		res = (scale.performDegreeToKey(degree) + root + semitone);
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
			{ this.degree = this.octaveSize; this.asc = [-1,0].choose; },
			{ this.degree = 0; this.asc = [1,0].choose; }
		);

		// vary rhythm?:
		if (3.rand > 0, // 2/3 chance to vary rhythm
			{  if (5.rand > 0, // 4/5 chance of eighths
				{ beatPhrase = [[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur*2/3],
					[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur/3]] },
					// else triplet
					{ beatPhrase = [[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur/3],
						[ModalBass.getRealPitch(this.degree + [4,this.octaveSize].choose, this.scale, this.root), this.dur/3],
						[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur/3]] }
				);
			},
			//else no variation
			{ beatPhrase = [[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur]] }
		);
		this.prev2 = this.prev;
		if (this.prev == this.degree, {"REPEATED NOTE!"});
		this.prev = this.degree;

		^beatPhrase;
	}

	makeSecondBeat {
		if ( (this.prev==0) && (4.rand==0),
			{ this.degree = this.octaveSize;  this.asc=[-1,0].choose },
			{ this.degree = this.chooseDefaultNote() } );

		this.prev2 = this.prev;
		this.prev = this.degree;
		^[[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur]];
	}

	chooseDefaultNote {
		// returns degree only, does not change globals
		var degree;
		case
	// ascending
		{ this.asc == 1 }
		{ degree = [this.prev+1,this.prev+2,this.prev+3,this.prev-1,this.charNote].wchoose([0.7,0.1,0.05,0.05,0.1]);}
	// descending
		{ this.asc == -1 }
		{ degree = [this.prev-1,this.prev-2,this.prev-3,this.prev+1,this.charNote].wchoose([0.7,0.1,0.05,0.05,0.1]);}
	// direction not set, in lower octave
		{ (this.asc == 0) && (this.prev < this.octaveSize) }
		{ degree = [this.prev+1,this.prev+2,this.prev+3,this.prev-1,this.charNote].wchoose([0.4,0.2,0.1,0.1,0.2]);}
	// direction not set, in upper octave
		{ (this.asc == 0) && (this.prev >= this.octaveSize) }
		{ degree = [this.prev-1,this.prev-2,this.prev-3,this.prev+1,this.charNote].wchoose([0.4,0.2,0.1,0.1,0.2]); };

		^degree;
	}

	chooseFinalNote {
		// returns degree only, does not change globals
		var degree, octave = (this.degree/this.octaveSize).asInteger, distance, direction, chromatic = 0, debug;

		// change tonic early if mode/root is changing next phrase
		if (this.onDeck != [this.scale, this.root], {this.setScale(this.onDeck[0], this.onDeck[1]);});

		distance = this.distanceFromTonic(this.degree);

		if (distance > 0, {direction = 1 }, {direction = -1});
		if ((this.degree > 0 ) && (direction < 0), {octave = octave +1 }); //hacky

		case
		{ abs(distance) > 2 }  { degree = [-1,1].choose; debug = 0}
		{ abs(distance) == 1 } { degree = (-1 * direction); debug =1 } // change direction
		{ abs(distance) == 0.5 } {degree = (-1 * direction); debug =2 }
		{ abs(distance) == 0 } { degree = [-1,1].choose; debug =3}
		{ abs(distance) == 2 } {
			if (this.shouldUseChromatic,
				{ degree = 0.5 * direction; chromatic = 1; debug = 4; },
				{ degree = (-1 * direction);debug = 5; } );
			};


		this.updateUsedChromatic(chromatic);
		//restore original octave:
		degree = degree + (this.octaveSize * octave);
		// ["Took Path", debug].postln;

		^degree;

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
		this.degree = this.chooseDefaultNote();
		this.degree = this.preventRootOrRepeat(this.degree, this.prev, this.prev2);

		this.prev2 = this.prev;
		this.prev = this.degree;
		^[[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur]];
	}

	makeLastBeat {
		// force a leading tone (above or below)
		if (this.skipLastBeat, {this.skipLastBeat = false;  ^[0,0] });
		this.degree = this.chooseFinalNote();

		this.prev2 = this.prev;
		this.prev = this.degree;
		^[[ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur]];
	}

	make2ndLastBeat {
		var phrase;
		if (6.rand == 0,
			{
				this.skipLastBeat = true;
				// 1st in triplet
				this.degree = this.chooseDefaultNote();
				this.degree = this.preventRootOrRepeat(this.degree, this.prev, this.prev2);
				phrase = phrase.add([ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.degree;

				// 2nd in triplet

				this.degree = this.chooseDefaultNote();
				this.degree = this.preventRootOrRepeat(this.degree, this.prev, this.prev2);
				phrase = phrase.add([ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.degree;

				// 3rd in triplet (set to upper or lower leading tone)
				this.degree = this.chooseFinalNote;
				phrase = phrase.add([ModalBass.getRealPitch(this.degree, this.scale, this.root), this.dur*2/3]);
				this.prev2 = this.prev;
				this.prev = this.degree;
			}, {phrase = this.makeDefaultBeat()}
		);
		this.prev2 = this.prev;
		this.prev = this.degree;
		^phrase;
	}

	distanceFromTonic { |degree|
		// called in chooseFinalNote
		var interval, releventTonic, semitone = 0;
		if ((degree % 1) > 0, {semitone = 1});
		interval = (this.scale[degree] - this.scale[0]);

		if ( interval > 6, {interval = (interval - 12 - semitone)},
			{interval = interval + semitone});
		^interval;
	}

	calculateInterval {}




	preventRootOrRepeat { |degree, prev, prev2|
		while( {((degree % this.octaveSize) == 0) || (degree == prev) || (degree == prev2) },
			{ degree = [degree + 1, degree - 1].choose; }
		);
		^degree;

	}
	makePhrase{
		var phrase = [];
		this.handleChanges(); // schedule behaviour change
		case
		{ this.behaviour == \playNormal }  { phrase = this.normalBehaviour }
		{ this.behaviour == \playDegreeScore } { phrase = this.playDegreeScore };

		if (this.verbose, { this.displayPhrase(phrase) });
		^phrase;
	}
	handleChanges {
		if (this.beatCounter == 0, {
			case
			{this.changeToScore == true} {this.behaviour = \playDegreeScore; this.changeToScore = false;}
		});

	}

	normalBehaviour {
		var phrase = [], next;

		case
			{this.beatCounter == 0} { next = this.makeFirstBeat }
			{this.beatCounter == 1} { next = this.makeSecondBeat }
			{this.beatCounter == (this.phraseLength - 2) } { next = this.make2ndLastBeat() }
			{this.beatCounter == (this.phraseLength - 1) } { next = this.makeLastBeat() }
			{next = this.makeDefaultBeat }; // default case

		if (this.skipLastBeat, {this.skipLastBeat = false; this.beatCounter = this.beatCounter + 1; });
		this.beatCounter = this.beatCounter + 1;
		this.beatCounter = this.beatCounter % this.phraseLength;
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
		["inscale", inScale, "inRoot", inRoot, ModalBass.getNoteName(inRoot),"degree", startNote, "raw", rawPitch, ModalBass.getNoteName(rawPitch)].postln;
		outNote = ModalBass.getDegreeFromPitch(rawPitch, outScale, outRoot);
		["ouscale", outScale, "outRoot", outRoot, ModalBass.getNoteName(outRoot),"outnote", outNote, "raw", rawPitch, ModalBass.getNoteName(inRoot)].postln;
		testedPitch = ModalBass.getRealPitch(outNote, outScale, outRoot);
		[testedPitch == rawPitch, "raw", rawPitch, ModalBass.getNoteName(inRoot),"result", testedPitch, ModalBass.getNoteName(rawPitch),ModalBass.getNoteName(testedPitch)].postln;

	}
	*getNoteName { |midiNote|
		var pitchClass = ["C", "C#/Db", "D", "Eb", "E","F", "F#/Gb", "G", "G#/Ab","A","A#/Bb","B"], octave, outString;
		octave = (midiNote/12).asInteger;
		if (midiNote < 0, {octave = octave -1});
		if (midiNote.isInteger, { outString = pitchClass.wrapAt(midiNote) ++ octave.asString}, {"rest"});
		^(outString);

	}

	playDegreeScore {
		// set this.behaviour = \playDegreeScore (called in this.makePhrase)
		var outPhrase = [], score, scale = this.scoreBag[1], root = this.scoreBag[2], octave = this.scoreBag[3];
		score = this.scoreBag[0];
		if (score == nil, {scale = this.scale});
		if (root == nil, {root = this.root});
		if (octave == nil, {octave = 0});
		score.do { |event|
			var note;
			if (event[0] == \rest,
				{note = \rest},
				{note = (ModalBass.getRealPitch(event[0], scale, root) + (octave*12))});
			outPhrase = outPhrase.add([note, event[1]]);
		}
		^outPhrase;
	}

}