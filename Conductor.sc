// Conductor {
// 	var <>tempoclock, <>bass, <>drums, <>midiout;
// 	classvar <>static;
// 	*new { |tempo, midiout|
//
// 	^super.new.init(tempo, midiout) }
// 	init { |tempo, midiout|
// 		this.midiout = midiout;
// 		if (tempo == nil,
// 		{ this.tempoclock = TempoClock.new(132/60) }, {this.tempoclock = TempoClock.new(tempo) } );
// 		this.bass = ModalBass(Scale.dorian,root:2,phraseLength:8, midiout:this.midiout, tempoclock: this.tempoclock);
// 		this.drums = DrumPlayer(midiout: this.midiout, tempoclock: this.tempoclock);
//
// 	}
// 	play {
//
// 		this.tempoclock.reset;
// 		this.bass.playPhrases;
// 		this.drums.playPattern;
//
//
// 	}
//
// 	executeChart { |chordChart|
// 		// entries in the form [scale, root, numPatterns]
// 		var expandedChart = [], mainSched, onDeckChart, onDeckSched;
// 		chordChart.do { |event, i|
// 			var counter = 1;
// 			expandedChart = expandedChart.add([event[0], event[1]]);
// 			while ({counter < event[2]},
// 				{ expandedChart = expandedChart.add([]);
// 			counter = counter + 1} );
// 		};
// 		onDeckChart = expandedChart.rotate(-1);
//
//
// 		mainSched = Routine({expandedChart.do {|oneBar|
// 		oneBar.yield}});
//
// 		onDeckSched = Routine({onDeckChart.do {|oneBar|
// 		oneBar.yield}});
//
// 		this.tempoclock.schedAbs(0,{ var nextEvent = mainSched.next, nextOnDeck = onDeckSched.next;
// 			if (nextEvent == nil,
// 				{ mainSched.reset; nextEvent = mainSched.next;
// 			onDeckSched.reset; nextOnDeck = onDeckSched.next;});
//
// 		this.handleChartRoutine(nextEvent, nextOnDeck); 8 });
// 	}
//
// 	handleChartRoutine { |event, onDeckEvent|
// 		// [this.tempoclock.beats, event, onDeckEvent].postln;
// 		/*	if (event != [], { //["C - change to", event[0], event[1]].postln;
// 		this.bass.setScale(event[0], event[1]); });*/
// 		if (onDeckEvent != [], { //["C - onDeck to", onDeckEvent[0], onDeckEvent[1]].postln;
// 		this.bass.prepareNextMode(onDeckEvent[0], onDeckEvent[1]); } );
// 	}
//
// }
Conductor {
	var <>tempoclock, <>bass, <>drums, <>midiout;
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
		this.bass.playPhrases;
		this.drums.playPattern;


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


		mainSched = Routine({expandedChart.do {|oneBar|
			oneBar.yield}});

		onDeckSched = Routine({onDeckChart.do {|oneBar|
			oneBar.yield}});

		this.tempoclock.schedAbs(0,{ var nextEvent = mainSched.next, nextOnDeck = onDeckSched.next;
			if (nextEvent == nil,
				{ mainSched.reset; nextEvent = mainSched.next;
				  onDeckSched.reset; nextOnDeck = onDeckSched.next;});

			this.handleChartRoutine(nextEvent, nextOnDeck); 8 });
	}

	handleChartRoutine { |event, onDeckEvent|
		// [this.tempoclock.beats, event, onDeckEvent].postln;
	/*	if (event != [], { //["C - change to", event[0], event[1]].postln;
			this.bass.setScale(event[0], event[1]); });*/
		if (onDeckEvent != [], { //["C - onDeck to", onDeckEvent[0], onDeckEvent[1]].postln;
			this.bass.prepareNextMode(onDeckEvent[0], onDeckEvent[1]); } );
	}

}