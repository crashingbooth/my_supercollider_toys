DrumPattern {
	var  <>length, <>drumArray, <>name;
	classvar <>drumIndexDict, <>accentDict, <>drumList, <>swingRatio, <>eighths;

	*new { |name, length, drumParts, getDefaultRide =true|
	^super.new.init (name, length, drumParts, getDefaultRide) }

	*initClass {
		// amp of 0.5 will be default volume
		DrumPattern.accentDict = Dictionary.newFrom(List[\s, 0.62, \w, 0.3,  \n, 0.5, \vs, 0.8, \r, 0]);
		DrumPattern.drumList = ["kick","snare","ride","openhh", "closedhh","rim","midtom", "hightom"];
		DrumPattern.drumIndexDict = Dictionary();
		DrumPattern.drumList.do {|name, i| DrumPattern.drumIndexDict[name] = i} ;
		DrumPattern.setSwing(2.7);
	}

	init { |name = "unnamed", length = 4, drumParts, getDefaultRide|
		// amp of 0.5 will be default volume
		this.name = name;
		this.drumArray = Array.fill(DrumPattern.drumList.size,{[]});
		this.length = length;
		if (getDefaultRide, {
			this.getDefaultRideAndHat();});
		this.getMultipleDrums(drumParts);

	}

	at { |i|
		^this.drumArray[i]
	}
	*setSwing {|ratio|
		DrumPattern.swingRatio = ratio;
		DrumPattern.eighths = [DrumPattern.swingRatio/ (DrumPattern.swingRatio + 1), 1 / (DrumPattern.swingRatio + 1)];
	}


	getDefaultRideAndHat {
		this.addOneDrum("ride", [[1],[DrumPattern.eighths[0],\s],[DrumPattern.eighths[1]],[1],[DrumPattern.eighths[0],\s],[DrumPattern.eighths[1]]]);
		this.addOneDrum("closedhh", [[1,\r],[1,\s],[1,\r],[1,\s]])
	}

	addOneDrum { |drumName, drumList|
		var index, totalLength = 0;
		index = DrumPattern.drumIndexDict[drumName];

		this.drumArray[index] = [];
		drumList.do { |event|
			var accent = \n;
			if (event.size == 1, {accent = \n;}, {accent = event[1]});
			this.drumArray[index] = this.drumArray[index].add([event[0], DrumPattern.accentDict[accent]]);
			totalLength = totalLength + event[0];
		};
		if ((totalLength.equalWithPrecision(this.length, 0.01)) != true, {["part length does not match obj length:", totalLength, this.length,"drumlist", drumList].postln });
	}
	getMultipleDrums { |drumParts|
		drumParts.do { |drumLine|
			this.addOneDrum(drumLine[0], drumLine[1]);
		}
	}

	display {
		this.drumArray.do { |line, i|
			var lineStr = "";
			line.do { |vals|
				vals[0] = (vals.[0] + 0.00000001); // to prevent error for for short numbers :(
				lineStr =  lineStr ++ "(" ++ vals[0].asStringPrec(2) ++ ", " ++ vals[1].asString ++ "),";
			};
			lineStr = lineStr[0..(lineStr.size -2)];
			[DrumPattern.drumList[i], lineStr].postln;
		}
	}
}

DrumPlayer {
	var <>midiout, <>tempoclock, <>library, <>currentPattern, <>primaryPat, <>secondaryPat, <>barCount, <>playMode, <>varProb, <>rideLibrary, <>schedule, <>verbose, <>tempoclock, <>basicKSLibrary;
	classvar <>midiNumIndex;

	*new{ |midiout, tempoclock|
	^super.new.init (midiout, tempoclock) }
	*initClass {
		DrumPlayer.midiNumIndex = [36, 38, 51, 46, 42, 37, 45, 44]; //kick, snare, ride, open, closed, rim, lowtom, hightom
	}
	init { |midiout, tempoclock|
		this.midiout = midiout;
		if (tempoclock == nil, { this.tempoclock = TempoClock.new(132/60)}, { this.tempoclock = tempoclock });

		this.playMode = \playNormal; // or \playRandom, \playEntireLibrary, \playNormal, \playSingle
		this.barCount = 0;
		this.buildKSLibrary(); //kick -snare library

		this.buildRideLibrary();
		this.schedule = Routine({});
		this.currentPattern = DrumPlayer.build1BarPattern(this.basicKSLibrary[0]);
		this.primaryPat = this.basicKSLibrary[0];
		this.secondaryPat = this.basicKSLibrary[0];
		this.verbose = false;
		this.varProb = 0.5;
	}

	playRandomPatterns { |drumNumber|
		this.currentPattern = this.library.choose;
		this.currentPattern.name.postln;
	}

	*build1BarPattern { |ksLibraryIndex, ridePat = nil|
		// ksLibraryIndex should be full list from buildKSLIbrary, ridePat id eventlist only
		var outPattern;

		outPattern = DrumPattern.new(ksLibraryIndex[0],ksLibraryIndex[1],ksLibraryIndex[2]);
		if (ridePat != nil, { outPattern.addOneDrum("ride", ridePat[1])});
		^outPattern;

	}

	playEntireLibrary { |reps = 4|
		var getVal;
		getVal = (this.barCount / reps).asInteger;
		this.currentPattern  = DrumPlayer.build1BarPattern(this.basicKSLibrary.wrapAt(getVal));
		postf("currently playing % \n", this.currentPattern.name);
	}

	chooseByGamblersFallacy {
		var die = 1.0.rand, oldVarProb = this.varProb, newPattern;
		if (die > this.varProb,
			{ if (3.rand > 0,
				{newPattern = this.primaryPat.copy}, {newPattern = this.secondaryPat.copy});
				this.varProb = this.varProb + 0.1;

			}, {
				this.varProb = 0.5;
				newPattern = this.basicKSLibrary.choose;
				while ( { (newPattern == this.primaryPat) ||(newPattern == this.secondaryPat) },
				{newPattern = this.basicKSLibrary.choose;} )
			}
		);
		^newPattern;
	}
	playNormal {
		var next;
		next = this.schedule.next;
		postln(["barCount", this.barCount]);
		if ( next == nil,
			{	this.currentPattern = DrumPlayer.build1BarPattern(this.chooseByGamblersFallacy());
			if (this.verbose,{	this.currentPattern.name.postln });},
		{ this.currentPattern = next.copy; } );
		/*if (((this.barCount % 4) == 2) &&  (3.rand > 0),
		{this.currentPattern = DrumPlayer.generatePattern;});*/
		if ((this.barCount % 16) == 6, {this.elvinJonesFill(1)});
		if ((this.barCount % 16) == 13, {this.elvinJonesFill(2)});

	}

	playSingle {
		["playing", this.currentPattern.name].postln;
	}

	playRegenerateCustom {
		if ((this.barCount % 2) == 1,
		{ this.currentPattern = DrumPlayer.generatePattern });
	}

	scheduleRideVar {
		// schedule 2 bars using 2 bar ride variation
		var rideVar = this.rideLibrary.choose, twoPatterns = [nil,nil];

		2.do { |i|
			var ks;
			ks = this.chooseByGamblersFallacy();
			twoPatterns[i] = DrumPlayer.build1BarPattern(ks, rideVar[i]);

		};
		this.schedule = Routine({twoPatterns.do {|pattern| pattern.yield}});

	}


	decideNext {
		this.barCount = this.barCount + 1;
		case
		{ this.playMode == \playRandom} {this.playRandomPatterns()}
		{ this.playMode == \playEntireLibrary} {this.playEntireLibrary()}
		{ this.playMode == \playNormal} {this.playNormal()}
		{ this.playMode == \playSingle } {this.playSingle()}
		{ this.playMode == \playCustom } {this.playRegenerateCustom()};
		if (((this.barCount % 8) == 0), {["barCount", this.barCount].postln; this.scheduleRideVar()});


		if (this.verbose, {this.currentPattern.display();});
	}

	processPattern { |drumNumber|
		// Builds Pseq readable list from currentPattern - always called!
		// [midiNote, dur, amp] or [\rest, dur, 0]
		var drumLine, output = [];

		drumLine = this.currentPattern.[drumNumber];
		if (drumLine.size == 0, {output = [[\rest, this.currentPattern.length, 0]]}, {
			drumLine.do { |event, i| //event is [dur, amp (or rest symbol)]

				if (event[1] == 0, {output = output.add([\rest, event[0], 0])},
				{output = output.add([DrumPlayer.midiNumIndex[drumNumber], event[0], event[1]])})
		};});

		^output;
	}


	playPattern { |mode = nil|
		var pbs = [], beatsched;
		beatsched = BeatSched.new(tempoClock:this.tempoclock);
		beatsched.beat = 0;
		beatsched.qsched(3.99,{  this.decideNext; 4 });
		DrumPlayer.midiNumIndex.do { |drumNum, i|
			pbs = pbs.add( Pbind (
				\type, \midi,
				\midiout, this.midiout,
				[\midinote, \dur, \raw_amp], Pn(Plazy{Pseq(this.processPattern(i))}),
				\amp, Pkey(\raw_amp) + Pwhite(-0.02, 0.02),
				\chan, 1,
				\lag, Pwhite(-0.02, 0.02)
				).play(this.tempoclock);
		) };
	}
	*generatePattern {
		// ride will play quarters, make 6 slot array gen pattern from KSRH, either repeat or mutate
		// then build pattern from 12 slot array
		var choices = ["snare", "kick", "closedhh", "midtom", "hightom","rest"],
		initialArray = [], secondhalf, accent1 = 6.rand, accent2 = 6.rand;

		6.do { |i|
			var acc = \n;
			if ( (i == accent1) || (i == accent2), { acc = \s } );
			initialArray = initialArray.add([choices.choose, acc]);
		};
		secondhalf = initialArray.copy;

		if (2.rand == 0,
			{ var change1 = 6.rand, change2 = 6.rand;
				secondhalf[change1][0] = choices.choose;
		secondhalf[change2][0] = choices.choose; });



		^DrumPlayer.monoListToPattern(initialArray ++ secondhalf);
	}

	*monoListToPattern { |monoList, name = "generated pattern"|
		var drumArray, template, outPattern;
		drumArray = Array.fill (DrumPattern.drumList.size { [] });


		monoList.do { |event, stepNum|
			drumArray.do { |arraySoFar, arraySlotNum |
				if (DrumPattern.drumIndexDict[event[0].asString] == arraySlotNum,
					{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([1/3, event[1]]) },
				{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([1/3, \r]) } );
			}
		};
		template = [];
		DrumPattern.drumList.do { |drumName, i|
			template = template.add([drumName, drumArray[i]]);
		};

		outPattern = DrumPattern.new( name,monoList.size/3, template);
		outPattern.getDefaultRideAndHat();
		^outPattern
	}

	elvinJonesFill { |size = 2|
		var metersArr, inflatedArr = [], outPatternArr, numTrips = (12*size), drumOrder, drumCount = 0, drumArray;
		// drumOrder = ["hightom","midtom","snare"].scramble;
		drumOrder = ["hightom","midtom","snare"].scramble;
		outPatternArr = [];
		drumArray = Array.fill (DrumPattern.drumList.size { [] });
		metersArr = Array.fill (2, {5.rand  + 2 });
		while ({inflatedArr.size < numTrips},
			{
				metersArr.do { |currentMeter|
					inflatedArr = inflatedArr.add("acc");
					(1..(currentMeter-1)).do { inflatedArr = inflatedArr.add(drumOrder.wrapAt(drumCount)); drumCount = drumCount + 1; };
					// drumCount = drumCount + 1;
				}
		});

		// cut off extra
		inflatedArr = inflatedArr[0..(numTrips -1)];

		inflatedArr.do { |event, stepNum|
			var kick = 0, ride = 2;
			drumArray.do { |arraySoFar, arraySlotNum |
				if (DrumPattern.drumIndexDict[event.asString] == arraySlotNum,
					{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([1/3, \n]) },
				{ drumArray[arraySlotNum] = drumArray[arraySlotNum].add([1/3, \r]) } );

			};
			if (event == "acc", {
					drumArray[kick][drumArray[ride].size -1] = [1/3, \s];
					drumArray[ride][drumArray[ride].size -1] = [1/3, \s];
					});
		};


		size.do { |barNum|
			var template = [];
			DrumPattern.drumList.do {  |drumName, i|
				template = template.add([drumName, drumArray[i][(barNum * 12)..(barNum * 12 + 11)]]);

			};
			outPatternArr = outPatternArr.add(DrumPattern("elvin jones fill", 4, template, false));
		};
		// ^outPatternArr;
		this.schedule = Routine({outPatternArr.do {|pattern| pattern.yield}});

	}

	buildRideLibrary {
		var d, u; // i.e., downbeat, upbeat

		d = DrumPattern.eighths[0]; u = DrumPattern.eighths[1];
		this.rideLibrary = [
			// [0]
			[["ride", [[1],[d,\s],[u],[1],[d,\s],[u]]],
			["ride", [[1],[1,\s], [1],[1,\s]]] ] ,
			// [1]
			[["ride", [[1],[d,\s],[u],[1],[d,\s],[u]]],
			["ride", [[d],[u],[1,\s],[d],[u],[1,\s]]]],
			// [2]
			[["ride", [[1],[d,\s],[u],[1],[1,\s]]],
			["ride", [[d],[u],[1,\s],[1],[d,\s],[u]]]],
			[["ride", [[1],[d,\s],[u],[1],[1,\s]]],
			["ride", [[1],[1],[1],[1]]]]
		];
	}


	buildKSLibrary{
		// basic
		this.basicKSLibrary = [
			["pat 1",4,[
				["kick", [[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3],[1,\r]]],
				["snare", [[1,\r],[2/3,\s],[1/3],[1,\r],[2/3,\s],[1/3]]]
			] ],
			["pat 2", 4,[
				["kick", [[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3],[2/3,\r],[1/3]]],
				["snare", [[1,\r],[1,\s],[1,\r],[1,\s]]]
			] ],
			/*this.library = this.library.add(DrumPattern.new("pat 3", 4,[
			["snare", [[2 + (1/3),\r],[1/3,\s],[1/3],[1,\r]]]
			] ) );*/
			/*	this.library = this.library.add(DrumPattern.new("pat 4", 4,[
			["snare", [[1/3],[1/3],[(1/3),\r],[1,\r],[1/3],[1/3],[(1/3),\r],[1,\r]]],
			["kick", [[2/3,\r],[1/3],[1,\r],[2/3,\r],[1/3],[1,\r]]]
			] ) );*/
			["pat 5", 4,[
				["snare", [[2/3,\r],[1/3],[1/3,\r],[1/3],[1/3],[1,\r],[1,\s]]],
				["kick", [[2,\r],[2/3,\r],[1/3],[1,\r]]]
			] ],
			// comping
			["pat 6",4,[
				["kick", [[(1+(1/3))], [(1+(1/3))],[(1+(1/3))]]],
				["snare", [[2/3], [1 +(1/3)], [1 +(1/3)], [2/3]]]
			] ],
			["pat 7",4,[
				["kick", [[2],[2]]],
				["snare", [[2 +(1/3),\r], [1/3],[1 +(1/3)]]]
			] ],
			["pat 8, poly1",4,[
				["kick", [[(2/3),\r], [1], [1], [1],[(1/3)]]],
				["snare", [[1/3],[1 +(2/3)],[1/3],[1 +(2/3)] ]]
			] ],
			["pat 9",4,[
				["kick", [[(2/3),\r], [1 +(1/3)], [2/3], [1 +(1/3)]]],
				["snare", [[1,\r],[1 +(1/3),\s],[2/3],[1,\s] ]]
			] ],
			["pat 10",4,[
				["kick", [[1/3,\r],[2/3],[2/3,\s],[2/3],[2/3],[2/3,\s],[1/3]]],
				["snare", [[2/3],[2/3],[2/3],[2/3],[2/3],[2/3]]]
			] ],
			["pat 11a",4,[
				["kick", [[1/3],[1],[1/3],[1],[1/3],[1]]],
				["snare", [[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3]]]
			] ],
			["pat 11b",4,[
				["snare", [[1/3],[1],[1/3],[1],[1/3],[1]]],
				["kick", [[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3],[2/3,\r],[1/3],[1/3]]]
			] ]
			/*	this.library = this.library.add(DrumPattern.new("pat 11",4,[
			["kick", [[(1/3), \r],[2],[1 +(2/3)]]],
			["snare", [[1 +(1/3),\r], [2], [1/3,\s],[1/3]]]
			] ) );*/

		]; // end of library

	}



}
