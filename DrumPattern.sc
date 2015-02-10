DrumPattern {
	var  <>length, <>drumList, <>drumIndexDict, <>accentDict, <>drumArray;

	*new { |length, drumParts|
		^super.new.init (length, drumParts) }

	init { |length = 4, drumParts|
		// amp of 0.5 will be default volume
		this.accentDict = Dictionary.newFrom(List[\s, 0.62, \w, 0.3,  \n, 0.5, \vs, 0.8, \r, 0]);
		this.drumList = ["kick","snare","ride","openhh", "closedhh","rim"];
		this.drumIndexDict = Dictionary();
		this.drumList.do {|name, i|
			this.drumIndexDict[name] = i
		};
		this.drumArray = Array.fill(this.drumList.size,{[]});
		this.length = length;
		this.getDefaultRideAndHat();
		this.getMultipleDrums(drumParts);

	}
	at { |i|
		^this.drumArray[i]
	}

	getDefaultRideAndHat {
		this.addOneDrum("ride", [[1,\s,],[2/3],[1/3],[1,\s],[2/3],[1/3]]);
		this.addOneDrum("closedhh", [[1,\r],[1],[1,\r],[1]])
	}

	addOneDrum { |drumName, drumList|
		var index, totalLength = 0;
		index = this.drumIndexDict[drumName];
		drumList.do { |event|
			var accent = \n;
			if (event.size == 1, {accent = \n;}, {accent = event[1]});
			this.drumArray[index] = this.drumArray[index].add([event[0], this.accentDict[accent]]);
			totalLength = totalLength + event[0];
		};
		if (totalLength != this.length, {["part length does not match obj length:", totalLength, this.length,"drumlist", drumList].postln });
	}
	getMultipleDrums { |drumParts|
		drumParts.do { |drumLine|
			this.addOneDrum(drumLine[0], drumLine[1]);
		}
	}
}

DrumPlayer {
	var <>midiout, <>stretch, <>library, <>midiNumIndex, <>currentPattern, <>playRandom;

	*new{ |midiout, stretch|
		^super.new.init (midiout, stretch) }
	init { |midiout, stretch = 0.5|
		this.midiout = midiout;
		this.stretch = 0.45;

		this.playRandom = false;
		this.midiNumIndex = [36, 38, 51, 46, 42, 37]; //kick, snare, ride, open, closed, rim
		this.addToLibrary();
		this.currentPattern;
	}


	processPatternAt { |drumNumber, drumPattern|
		// [midiNote, dur, amp] or [\rest, dur, 0]
		var drumLine, output = [];

		if (this.playRandom && (drumNumber == 0),  {this.currentPattern = this.library.choose; this.currentPattern[0].postln });
		if (drumPattern == nil, {drumPattern = this.currentPattern});

		drumLine = drumPattern.[drumNumber];
		if (drumLine.size == 0, {output = [[\rest, drumPattern.length, 0]]}, {
			drumLine.do { |event, i| //event is [dur, amp (or rest symbol)]

				if (event[1] == 0, {output = output.add([\rest, event[0], 0])},
					{output = output.add([this.midiNumIndex[drumNumber], event[0], event[1]])})
		};});
		^output;
	}


	playPattern { |drumPattern|
		var pbs = [];
		this.midiNumIndex.do { |drumNum, i|
			pbs = pbs.add( Pbind (
				\type, \midi,
				\midiout, this.midiout,
				[\midinote, \dur, \raw_amp], Pn(Plazy{Pseq(this.processPatternAt(i))}),
				\amp, Pkey(\raw_amp) + Pwhite(-0.08, 0.05),
				\chan, 1,
				\stretch, this.stretch,
				\lag, Pwhite(-0.03, 0.03)
				).play;
		) };
	}

	addToLibrary{
		// basic
		this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3],[1,\r]]],
			["snare", [[1,\r],[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3]]]
		] ) );
		/*this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3]]],
		    ["snare", [[1,\r],[1,\s],[1,\r],[1,\s]]]
		] ) );*/
		this.library = this.library.add(DrumPattern.new(4,[
			["snare", [[2 + (1/3),\r],[1/3,\s],[1/3],[1,\r]]]
		] ) );
		/*this.library = this.library.add(DrumPattern.new(4,[
			["snare", [[1/3],[1/3],[(1/3),\r],[1,\r],[1/3],[1/3],[(1/3),\r],[1,\r]]],
			["kick", [[2/3,\r],[1/3],[1,\r],[2/3,\r],[1/3],[1,\r]]]
		] ) );*/
		this.library = this.library.add(DrumPattern.new(4,[
			["snare", [[2/3,\r],[1/3],[1/3,\r],[1/3],[1/3],[1,\r],[1,\s]]],
			["kick", [[2,\r],[2/3,\r],[1/3],[1,\r]]]
		] ) );
		// comping
		this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[(1+(1/3))], [(1+(1/3))],[(1+(1/3))]]],
			["snare", [[2/3], [1 +(1/3)], [1 +(1/3)], [2/3]]]
		] ) );
		this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[2],[2]]],
			["snare", [[2 +(1/3),\r], [1/3],[1 +(1/3)]]]
		] ) );
		this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[(2/3),\r], [1], [1], [1],[(1/3)]]],
			["snare", [[1/3],[1 +(2/3)],[1/3],[1 +(2/3)] ]]
		] ) );
	/*	this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[(2/3),\r], [1 +(1/3)], [2/3], [1 +(1/3)]]],
			["snare", [[1,\r],[1 +(1/3),\s],[2/3],[1,\s] ]]
		] ) );*/
		this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[1/3,\r],[2/3],[2/3,\s],[2/3],[2/3],[2/3,\s],[1/3]]],
			["snare", [[2/3],[2/3],[2/3],[2/3],[2/3],[2/3]]]
		] ) );
		this.library = this.library.add(DrumPattern.new(4,[
			["kick", [[(1/3), \r],[2],[1 +(2/3)]]],
			["snare", [[1 +(1/3),\r], [2], [1/3,\s],[1/3]]]
		] ) );



	}



}
