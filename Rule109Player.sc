Rule109Player {
	var <>ca, <>history, <>hypotheses, <>repThresh, <>haveGoodHypo;

	*new { |gridWidth, windowSize, repThresh,midiout|
	^super.new.init(gridWidth, windowSize, repThresh, midiout);
	}

	init { |gridWidth, windowSize, repThresh = 4, midiout|
		this.ca = ElemCA(gridWidth, 109, nil, midiout);
		this.ca.setWindow(windowSize, 0, false);
		this.ca.windowVals();
		this.ca.window.postln;
		this.repThresh = repThresh;
		this.history = [this.ca.window];
		this.haveGoodHypo = false;
		// this.history.postln;
	}

	doThang { |tempo = 1|
		var r;
		r = Routine.new(
			{loop {
				var current;
				this.ca.playNext();
				current = this.ca.window();
				this.reviewHypotheses(current);
				this.createHypotheses(current);
				this.history = this.history.add(current.copy);

				tempo.yield}
		}).play;
	}

	// hypotheses is a list of [start, line after endline, number of reps]
	// if current line consistant with hypothesis, nothing happens except update reps
	// when reps == 4, move window refresh history and hypotheses

	reviewHypotheses { |current|
		var remove = [];
		this.hypotheses.do { |hypo, i|
			var df = hypo[1] - hypo[0], end = hypo[1], reps = hypo[2], curLineNum = this.history.size, target;
			target = curLineNum - (df*reps);
			if (history[target] != current,
				{remove = remove.add(i)},
				{   if ((target - hypo[0]) == (df -1),
					{
						this.hypotheses[i][2] = this.hypotheses[i][2] + 1;
						if (df > 8, {this.haveGoodHypo = true; this.hypotheses[i] = this.hypotheses[i].add("flag")};);
						if (this.hypotheses[i][2] == this.repThresh, { this.moveAndReset(); remove = []});
			} );

			});
		};
		remove.do {|val|

			this.hypotheses.remove(val);
		}
	}
	createHypotheses { |current|
		//if history is empty???
		this.history.do { |entry, i|
			if (entry.asString == current.asString,
				{

						this.hypotheses = this.hypotheses.add([i, this.history.size, 1])
				}
			);
		}
	}
	moveAndReset {
		this.history = [];
		this.hypotheses = [];
		this.haveGoodHypo = false;
		this.ca.shiftWindow(1);
	}

}