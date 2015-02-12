DrumPattern {
	var  <>length, <>drumList, <>drumIndexDict, <>accentDict, <>drumArray, <>name, <>swingRatio, <>eighths;

	*new { |name, length, drumParts|
		^super.new.init (name, length, drumParts) }

	init { |name = "unnamed", length = 4, drumParts |
		// amp of 0.5 will be default volume
		this.accentDict = Dictionary.newFrom(List[\s, 0.62, \w, 0.3,  \n, 0.5, \vs, 0.8, \r, 0]);
		this.drumList = ["kick","snare","ride","openhh", "closedhh","rim"];
		this.name = name;
		this.drumIndexDict = Dictionary();

		this.setSwing(2.7);

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
	setSwing {|ratio|
		this.swingRatio = ratio;
		this.eighths = [this.swingRatio/ (this.swingRatio + 1), 1 / (this.swingRatio + 1)];
	}


	getDefaultRideAndHat {
		// this.addOneDrum("ride", [[1,\s,],[2/3],[1/3],[1,\s],[2/3],[1/3]]);
		this.addOneDrum("ride", [[1,\s,],[this.eighths[0]],[this.eighths[1]],[1,\s],[this.eighths[0]],[this.eighths[1]]]);
		this.addOneDrum("closedhh", [[1,\r],[1,\s],[1,\r],[1,\s]])
	}

	addOneDrum { |drumName, drumList|
		var index, totalLength = 0;
		index = this.drumIndexDict[drumName];
		this.drumArray[index] = [];
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
	var <>midiout, <>stretch, <>library, <>midiNumIndex, <>currentPattern, <>primaryPat, <>secondaryPat, <>barCount, <>playMode, <>backUpPattern, <>varProb;

	*new{ |midiout, stretch|
		^super.new.init (midiout, stretch) }
	init { |midiout, stretch = 0.5|
		this.midiout = midiout;
		this.stretch = 0.45;

		this.playMode = \playRandom; // or \playBiased, \playEntireLibrary
		this.barCount = 0;
		this.midiNumIndex = [36, 38, 51, 46, 42, 37]; //kick, snare, ride, open, closed, rim
		this.addToLibrary();
		this.backUpPattern = [];
		this.currentPattern;
		this.primaryPat = this.library[0];
		this.secondaryPat = this.library[1];
		this.varProb = 0.5;
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
		["Primary", this.primaryPat.name, "Secondary", this.secondaryPat.name].postln;

	}
	playBiasedPatterns { |drumNumber|
		// primary 50%, secondary 25%, all other 25%
		var die;
		if (this.primaryPat == nil, {this.setBiasPatterns});

		// change patterns when first drumline processed
		die = 5.rand;
		case
		{die < 2}  {this.currentPattern = this.primaryPat; "playing primary".postln}
		{die == 2} {this.currentPattern = this.secondaryPat; "playing secondary".postln}
		{die > 2} {this.currentPattern = this.library.choose; postf("playing % /n", this.currentPattern.name )};
		if (die > 2,
			{while ( { (this.currentPattern == this.primaryPat) ||(this.currentPattern == this.secondaryPat) },
					{this.currentPattern = this.library.choose} ) } );
	}
	playEntireLibrary { |drumNumber, reps = 4|
		this.currentPattern = this.library.wrapAt((this.barCount / reps).asInteger);
		this.barCount = this.barCount + 1;
		postf("currently playing % \n", this.currentPattern.name);
	}

	playGamblersFallacy {
		var die = 1.0.rand, oldVarProb = this.varProb;
		if (die > this.varProb,
			{ if (3.rand > 0,
				{this.currentPattern = this.primaryPat}, {this.currentPattern = this.secondaryPat});
				this.varProb = this.varProb + 0.1;

			}, {
				this.varProb = 0.5;
				this.currentPattern = this.library.choose;
				while ( { (this.currentPattern == this.primaryPat) ||(this.currentPattern == this.secondaryPat) },	{this.currentPattern = this.library.choose} )
			}

		);
		["chose", this.currentPattern.name, "die", die,"old",oldVarProb, "now",this.varProb].postln;
	}

	decideNext {

		case
		{ this.playMode == \playRandom} {"HIIIII".postln; this.playRandomPatterns()}
		{ this.playMode == \playBiased} {this.playBiasedPatterns()}
		{ this.playMode == \playEntireLibrary} {this.playEntireLibrary()}
		{ this.playMode == \playGamblersFallacy} {this.playGamblersFallacy()};
		if (8.rand == 0, {this.varyHihat()}, {this.currentPattern.getDefaultRideAndHat()});

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


	playPattern { |mode = nil|
		var pbs = [];

		this.midiNumIndex.do { |drumNum, i|
			pbs = pbs.add( Pbind (
				\type, \midi,
				\midiout, this.midiout,
				[\midinote, \dur, \raw_amp], Pn(Plazy{Pseq(this.processPattern(i))}),
				\amp, Pkey(\raw_amp) + Pwhite(-0.02, 0.02),
				\chan, 1,
				\stretch, this.stretch,
				\lag, Pwhite(-0.02, 0.02)
				).play;
		) };
	}
	varyHihat {
		var temp;
		temp = this.currentPattern.copy;
		// temp.addOneDrum("closedhh", [[2/3],[2/3],[2/3],[2/3],[2/3],[2/3]]);
		temp.addOneDrum("ride",  [[2/3,\s],[2/3,\s],[2/3,\s],[1,\s,],[2/3],[1/3]]);
		"ridevar".postln;
		this.currentPattern = temp.copy;

	}
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
	/*	this.library = this.library.add(DrumPattern.new("pat 4", 4,[
			["snare", [[1/3],[1/3],[(1/3),\r],[1,\r],[1/3],[1/3],[(1/3),\r],[1,\r]]],
			["kick", [[2/3,\r],[1/3],[1,\r],[2/3,\r],[1/3],[1,\r]]]
		] ) );*/
		this.library = this.library.add(DrumPattern.new("pat 5", 4,[
			["snare", [[2/3,\r],[1/3],[1/3,\r],[1/3],[1/3],[1,\r],[1,\s]]],
			["kick", [[2,\r],[2/3,\r],[1/3],[1,\r]]]
		] ) );
		// comping
		this.library = this.library.add(DrumPattern.new("pat 6",4,[
			["kick", [[(1+(1/3))], [(1+(1/3))],[(1+(1/3))]]],
			["snare", [[2/3], [1 +(1/3)], [1 +(1/3)], [2/3]]]
		] ) );
	/*	this.library = this.library.add(DrumPattern.new("pat 7",4,[
			["kick", [[2],[2]]],
			["snare", [[2 +(1/3),\r], [1/3],[1 +(1/3)]]]
		] ) );*/
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