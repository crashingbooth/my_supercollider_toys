MIDItoMarkov {
	var <>midifile, // initial midifile
	<>tempoMod, // assume 1 = 120 bpm?
	<>events,   // noteEvents for midifile
	<>queue, // events that have received notes on but not off
	<>output,  // to be returned
	<>error, // unmatched notes
	<>restStart = nil; // if queue is empty
	*new {|midifile, tempoMod|
		^super.new.init(midifile, tempoMod) }
	init { |midifile, tempoMod = 1|
		this.tempoMod = tempoMod;
		this.midifile = midifile;

		this.output = [];
		this.queue = List.new();
		this.restStart = nil;
		this.error = [];

		this.events = midifile.noteEvents;
		this.events.do {|event| this.processEvent(event);};
		this.reprocessTimes();
		^this.output;
	}
	processEvent {|event|
		var type = event[2], time = event[1], note = event[4];
		switch (type,
			\noteOn, {
				if (this.restStart != nil,
					{   var delta =  time - this.restStart;
						if (delta < 0, {
						this.output = this.output.add([\rest, time - this.restStart]);
							this.restStart = nil; } ); } ;	);
				this.queue = this.queue.add([note, time]);
			},
			\noteOff, {
				var index;
				index = this.findInQueue(note);
				if (index != nil,
					{
						this.output = this.output.add([(note-36), time - this.queue[index][1]]);
						this.queue.removeAt(index);
						if (this.queue.size == 0,  {this.restStart = time });

					},
					{
						postln(["error, unmatched noteOff", note, time]);
						this.error = this.error.add(note, time);

					}
				);

			};
		)
	}
	// returns index of this.queue, if not found returns nil
	findInQueue { |note|
		var result = nil;
		this.queue.do { |pair, i|
			if (pair[0] == note, { result = i })
		};
		^result;
	}
	reprocessTimes {
		this.output.do { |event, i|
			this.output[i][1] = event[1]/2048; }
	}
}