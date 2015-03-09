Conductor {
	var <>tempoclock, <>bass, <>drums, <>midiout, <>drumPbind, <>bassPbind, <>piano, <>pianoPbind ;
	classvar <>static;
	*new { |tempo, midiout|

	^super.new.init(tempo, midiout) }
	init { |tempo, midiout|
		this.midiout = midiout;
		if (tempo == nil,
		{ this.tempoclock = TempoClock.new(132/60) }, {this.tempoclock = TempoClock.new(tempo) } );
		this.bass = ModalBass(Scale.dorian,root:2,phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
		this.drums = DrumPlayer(midiout: this.midiout, tempoclock: this.tempoclock);
		this.piano = ModalPiano(Scale.dorian,root:2, midiout:this.midiout, tempoclock: this.tempoclock);

	}
	play {

		this.tempoclock.reset;
		this.bassPbind = this.bass.play;
		this.drumPbind = this.drums.play;
		this.pianoPbind = this.piano.play;


	}


	chooseScaleRoot {
		var scale, root;
		scale = [Scale.ionian, Scale.dorian, Scale.phrygian, Scale.lydian, Scale.mixolydian, Scale.aeolian, Scale.locrian].wchoose([0.1,0.4,0.2,0.3,0.2,0.2,0.1]);
		root = [-8,-7,-5,-3,-1,0,2].choose;
		["NEXT SCALE WILL BE", scale, ModalBass.getNoteName(root)].postln;
		^[scale, root];
	}

	doOstinato {
		var bassScore;
		bassScore = [Conductor.generateOstinato(), Scale.dorian, 2, 0 ];
		this.bass.scoreBag = bassScore;
		this.bass.changeToScore = true;
		this.drums.playMode = \playRegFill;
		this.tempoclock.sched(31, { "called".postln; this.drums.playMode = \playRegularPolymetric });
	}

	executeChart { |chordChart|
		// entries in the form [scale, root, numPatterns]
		var expandedChart = [], mainSched, onDeckChart, onDeckSched;
		chordChart.do { |event, i|
			var counter = 1;
			expandedChart = expandedChart.add([event[0], event[1]]);
			while ({counter < event[2]},
				{ expandedChart = expandedChart.add([]);
			counter = counter + 1} );
		};
		onDeckChart = expandedChart.rotate(-1);

		// set up first note
		if (this.bassPbind != nil , {this.bassPbind.stop});
		if (this.drumPbind != nil , {this.drumPbind.stop});
		if (this.pianoPbind != nil , {this.pianoPbind.stop});
		this.bass = ModalBass(expandedChart[0][0],root:expandedChart[0][1],phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
		this.piano = ModalPiano(expandedChart[0][0],root:expandedChart[0][1], midiout:this.midiout, tempoclock: this.tempoclock);
		// this.bassPbind = this.bass.play;
		this.play;
		mainSched = Routine({expandedChart.do {|oneBar|
		oneBar.yield}});
		onDeckChart.postln;
		expandedChart.postln;
		onDeckSched = Routine({onDeckChart.do {|oneBar|
		oneBar.yield}});

		this.tempoclock.schedAbs(0,{ var nextEvent = mainSched.next, nextOnDeck = onDeckSched.next;
			if (nextEvent == nil,
				{ mainSched.reset; nextEvent = mainSched.next;
			onDeckSched.reset; nextOnDeck = onDeckSched.next;});

		this.handleChartRoutine( nextOnDeck); 8 });
	}

	handleChartRoutine { |onDeckEvent|
		if (onDeckEvent != [], { ["C - onDeck to", onDeckEvent[0], onDeckEvent[1]].postln;
		this.bass.prepareNextMode(onDeckEvent[0], onDeckEvent[1]); this.piano.prepareNextMode(onDeckEvent[0], onDeckEvent[1]); } );
	}

	changeRandomly { |duration = 4, startScale, startRoot = 2|
		var chart = [], onDeck, onDeckSched;
		if (startScale == nil, {startScale = Scale.dorian});
		if ((startScale != this.bass.scale) || (startRoot != this.bass.root), {
			if (this.bassPbind != nil , {this.bassPbind.stop});
			this.bass = ModalBass(startScale,root:startRoot,phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
			this.bassPbind = this.bass.play;
		});

		// onDeckSched = changeRandomSelect
		this.tempoclock.schedAbs(0,{ var nextOnDeck = onDeckSched.next;
			if (nextOnDeck == nil,
				{ onDeckSched = this.changeRandomSelect(duration); nextOnDeck = onDeckSched.next;});

		this.handleChartRoutine(nextOnDeck); 8 });

	}
	changeRandomSelect { |duration|
		var onDeck, chart = [];
		onDeck = this.chooseScaleRoot();
		chart = Array.fill(duration -1, {[]});
		chart = chart.add(onDeck);
		^Routine({chart.do {|oneBar| oneBar.yield}});
	}

	*generateOstinato  {
		var downBeat = ["firstHalf", "secondHalf"].choose,
		positions = [[0,2].choose, [4,6].choose],
		startDur, details, variation,
		outScore = [];
		positions.dopostln;
		if (downBeat == "secondHalf",
			{positions[0] = positions[0] + 1 },
			{positions[1] = positions[1] + 1 });
		positions.dopostln;
		positions.do {|pos, i|
			positions[i] = Conductor.eigthsPosToTripletsPos(pos);
		};


		positions.dopostln;
		outScore = Conductor.scoreFromPositions(positions);

		variation = Conductor.varyOstinato(positions);
		if (2.rand == 0,
			{^(outScore ++ variation ++outScore ++ variation)},
			{^(outScore ++ outScore ++ variation ++ outScore)});
		// ^(outScore ++ outScore ++ outScore ++ outScore);


	}
	*scoreFromPositions { |positions|
		var outScore = [], durations = [];

		positions.do {|position, i|
			// ["TILLHERE",positions.size].postln;
			if (i != (positions.size - 1), {
				durations = durations.add( positions[i+1] - positions[i]);
			});
		};

		durations = durations.add((12 + (12 -  positions[positions.size-1])));
		outScore = outScore.add([\rest, positions[0]*(1/3)]);
		durations.do { |duration|
			outScore = outScore.add([[0,7].choose, duration*(1/3)]);
		};
		^outScore;

	}
	*varyOstinato { |original|
		var gaps = [original[0], original[1] -original[0], 12 - original[1]], maxV, maxI, variation, newNote, outVar, dur;


		maxI = gaps.maxIndex;
		if (maxI.size > 1, {maxI = maxI.choose});
		maxV = gaps[maxI];

		variation = original.copy;
		dur = div(maxV,2);

		case
		{maxI == 0} {newNote = (original[0] - dur); variation.insert(0,newNote)}
		{maxI == 1} {newNote = (original[1] - dur); variation.insert(1,newNote)}
		{maxI == 2} {newNote = (original[1] + dur); variation = variation.add(newNote)};
		variation.postln;


		outVar = Conductor.scoreFromPositions(variation);
		// outVar[2] = [[-1,2,-3,4, 6].choose, outVar[1][1]];
		^outVar;


	}


	*eigthsPosToTripletsPos {|eighth|
		var outVal;
		if (eighth % 2 == 0,
			{outVal = eighth + (eighth * 0.5)},
			{outVal = eighth + ((eighth + 1) *0.5)});
		^outVal;
	}

}
