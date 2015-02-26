Conductor {
	var <>tempoclock, <>bass, <>drums, <>midiout, <>drumPbind, <>bassPbind ;
	classvar <>static;
	*new { |tempo, midiout|

	^super.new.init(tempo, midiout) }
	init { |tempo, midiout|
		this.midiout = midiout;
		if (tempo == nil,
		{ this.tempoclock = TempoClock.new(132/60) }, {this.tempoclock = TempoClock.new(tempo) } );
		this.bass = ModalBass(Scale.dorian,root:2,phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
		this.drums = DrumPlayer(midiout: this.midiout, tempoclock: this.tempoclock);

	}
	play {

		this.tempoclock.reset;
		this.bassPbind = this.bass.play;
		this.drumPbind = this.drums.play;


	}

	chooseScaleRoot {
		var scale, root;
		scale = [Scale.ionian, Scale.dorian, Scale.phrygian, Scale.lydian, Scale.mixolydian, Scale.aeolian, Scale.locrian].wchoose([0.1,0.4,0.2,0.3,0.2,0.2,0.1]);
		root = [-8,-7,-5,-3,1,0,2].choose;
		["NEXT SCALE WILL BE", scale, ModalBass.getNoteName(root)].postln;
		^[scale, root];
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
		this.bass = ModalBass(expandedChart[0][0],root:expandedChart[0][1],phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
		this.bassPbind = this.bass.play;
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

		this.handleChartRoutine(nextEvent, nextOnDeck); 8 });
	}

	handleChartRoutine { |onDeckEvent|
		if (onDeckEvent != [], { ["C - onDeck to", onDeckEvent[0], onDeckEvent[1]].postln;
		this.bass.prepareNextMode(onDeckEvent[0], onDeckEvent[1]); } );
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

}
