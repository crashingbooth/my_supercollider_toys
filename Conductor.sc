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
		var expandedChart = [], sched, beatsched;
		chordChart.do { |event, i|
			var counter = 1;
			expandedChart = expandedChart.add([event[0], event[1]]);
			while ({counter < event[2]},
				{ expandedChart = expandedChart.add([]);
					counter = counter + 1} );
		};

		sched = Routine({expandedChart.do {|oneBar|
			oneBar.yield}});

		this.tempoclock.schedAbs(4,{ var nextEvent = sched.next;
			if (nextEvent == nil,  {sched.reset; nextEvent = sched.next});
			this.handleChartRoutine(nextEvent); 8 });
	}

	handleChartRoutine { |event, routine|

		if (event != [], {this.bass.setScale(event[0], event[1])});

	}

}
