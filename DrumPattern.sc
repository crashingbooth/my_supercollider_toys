DrumPattern {
	var  <>length, <>drumList, <>drumIndexDict, <>accentDict, <>drumArray, <>name;

	*new { |name, length, drumParts|
		^super.new.init (name, length, drumParts) }

	init { |name = "unnamed", length = 4, drumParts |
		// amp of 0.5 will be default volume
		this.accentDict = Dictionary.newFrom(List[\s, 0.62, \w, 0.3,  \n, 0.5, \vs, 0.8, \r, 0]);
		this.drumList = ["kick","snare","ride","openhh", "closedhh","rim"];
		this.name = name;
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
	var <>midiout, <>stretch, <>library, <>midiNumIndex, <>currentPattern, <>primaryPat, <>secondaryPat, <>barCount, <>playMode;

	*new{ |midiout, stretch|
		^super.new.init (midiout, stretch) }
	init { |midiout, stretch = 0.5|
		this.midiout = midiout;
		this.stretch = 0.45;
		this.primaryPat = nil;
		this.secondaryPat = nil;
		this.playMode = \playRandom; // or \playBiased, \playEntireLibrary
		this.barCount = 0;
		this.midiNumIndex = [36, 38, 51, 46, 42, 37]; //kick, snare, ride, open, closed, rim
		this.addToLibrary();
		this.currentPattern;
	}

	playRandomPatterns { |drumNumber|
		this.currentPattern = this.library.choose;
		this.currentPattern.name.postln;
	}

	setBiasPatterns { |primary, secondary|
		if (primary == nil,
			{ this.primaryPat = this.library.choose},
			{ this.primaryPat = primary } );
		if (secondary == nil,
			{ this.secondaryPat = this.library.choose},
			{ this.secondaryPat = secondary } );
		while ( {this.primaryPat == this.secondaryPat }, { this.secondaryPat = this.library.choose } );
		["Primary", this.primaryPat, "Secondary", this.secondaryPat].postln;

	}
	playBiasedPatterns { |drumNumber|
		// primary 50%, secondary 25%, all other 25%
		var die;
		if (this.primaryPat == nil, {this.setBiasPatterns});

		// change patterns when first drumline processed
		die = 4.rand;
		case
		{die < 2}  {this.currentPattern = this.primaryPat; "playing primary".postln}
		{die == 2} {this.currentPattern = this.secondaryPat; "playing secondary".postln}
		{die == 3} {this.currentPattern = this.library.choose; "playing other".postln};
		if (die == 3,
			{while ( { (this.currentPattern == this.primaryPat) ||(this.currentPattern == this.secondaryPat) },
					{this.currentPattern = this.library.choose} ) } );
	}

	decideNext {
		case
		{ this.playMode == \playRandom} {"HIIIII".postln; this.playRandomPatterns()}
		{ this.playMode == \playBiased} {this.playBiasedPatterns()}
		{ this.playMode == \playEntireLibrary} {this.playEntireLibrary()}

	}
	processPattern { |drumNumber|
		// Builds Pseq readable list from currentPattern - always called! also changes curPATTERN
		// [midiNote, dur, amp] or [\rest, dur, 0]
		var drumLine, output = [];

		// change this.cuurentPattern based on this.playMode
		if (drumNumber == 0, {this.decideNext});

		drumLine = this.currentPattern.[drumNumber];
		if (drumLine.size == 0, {output = [[\rest, this.currentPattern.length, 0]]}, {
			drumLine.do { |event, i| //event is [dur, amp (or rest symbol)]

				if (event[1] == 0, {output = output.add([\rest, event[0], 0])},
					{output = output.add([this.midiNumIndex[drumNumber], event[0], event[1]])})
		};});
		^output;
	}

	playEntireLibrary { |drumNumber, reps = 8|
		this.currentPattern = this.library.wrapAt((this.barCount / reps).asInteger);
		this.barCount = this.barCount + 1;
		postf("currently playing % \n", this.currentPattern.name);
	}

	playPattern { |mode = nil|
		var pbs = [];

		this.midiNumIndex.do { |drumNum, i|
			pbs = pbs.add( Pbind (
				\type, \midi,
				\midiout, this.midiout,
				[\midinote, \dur, \raw_amp], Pn(Plazy{Pseq(this.processPattern(i))}),
				\amp, Pkey(\raw_amp) + Pwhite(-0.08, 0.05),
				\chan, 1,
				\stretch, this.stretch,
				\lag, Pwhite(-0.03, 0.03)
				).play;
		) };
	}
	/*varyHihat {
		this.currentPattern[
	}*/

	addToLibrary{
		// basic
		this.library = this.library.add(DrumPattern.new("pat 1",4,[
			["kick", [[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3],[1,\r]]],
			["snare", [[1,\r],[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3]]]
		] ) );
		this.library = this.library.add(DrumPattern.new("pat 2", 4,[
			["kick", [[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3]]],
		    ["snare", [[1,\r],[1,\s],[1,\r],[1,\s]]]
		] ) );
		/*this.library = this.library.add(DrumPattern.new("pat 3", 4,[
			["snare", [[2 + (1/3),\r],[1/3,\s],[1/3],[1,\r]]]
		] ) );*/
		this.library = this.library.add(DrumPattern.new("pat 4", 4,[
			["snare", [[1/3],[1/3],[(1/3),\r],[1,\r],[1/3],[1/3],[(1/3),\r],[1,\r]]],
			["kick", [[2/3,\r],[1/3],[1,\r],[2/3,\r],[1/3],[1,\r]]]
		] ) );
		this.library = this.library.add(DrumPattern.new("pat 5", 4,[
			["snare", [[2/3,\r],[1/3],[1/3,\r],[1/3],[1/3],[1,\r],[1,\s]]],
			["kick", [[2,\r],[2/3,\r],[1/3],[1,\r]]]
		] ) );
		// comping
		this.library = this.library.add(DrumPattern.new("pat 6",4,[
			["kick", [[(1+(1/3))], [(1+(1/3))],[(1+(1/3))]]],
			["snare", [[2/3], [1 +(1/3)], [1 +(1/3)], [2/3]]]
		] ) );
		this.library = this.library.add(DrumPattern.new("pat 7",4,[
			["kick", [[2],[2]]],
			["snare", [[2 +(1/3),\r], [1/3],[1 +(1/3)]]]
		] ) );
		this.library = this.library.add(DrumPattern.new("pat 8, poly1",4,[
			["kick", [[(2/3),\r], [1], [1], [1],[(1/3)]]],
			["snare", [[1/3],[1 +(2/3)],[1/3],[1 +(2/3)] ]]
		] ) );
		this.library = this.library.add(DrumPattern.new("pat 9",4,[
			["kick", [[(2/3),\r], [1 +(1/3)], [2/3], [1 +(1/3)]]],
			["snare", [[1,\r],[1 +(1/3),\s],[2/3],[1,\s] ]]
		] ) );
		this.library = this.library.add(DrumPattern.new("pat 10",4,[
			["kick", [[1/3,\r],[2/3],[2/3,\s],[2/3],[2/3],[2/3,\s],[1/3]]],
			["snare", [[2/3],[2/3],[2/3],[2/3],[2/3],[2/3]]]
		] ) );
	/*	this.library = this.library.add(DrumPattern.new("pat 11",4,[
			["kick", [[(1/3), \r],[2],[1 +(2/3)]]],
			["snare", [[1 +(1/3),\r], [2], [1/3,\s],[1/3]]]
		] ) );*/



	}



}