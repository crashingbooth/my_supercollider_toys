/*
class for generating binary rhythm patterns by various menus and manipulating them


*/
PolyDrumBinary {
	var <>length, <>channel, <>method, <>curLine, <>prevLine, <>clock, <>midiOut, <>id, <>pat, <>favLine, <>stretch;
	classvar <>count;
	// call constructor
	*new { |length, channel, clock, midiOut, stretch|
		^super.new.init(length, channel, clock, midiOut, stretch);
	}

	// constructor
	init {|length, channel, clock, midiOut, stretch = 1|
		if (PolyDrumBinary.count == nil,
			{ PolyDrumBinary.count = 1 },
			{ PolyDrumBinary.count = PolyDrumBinary.count + 1 } );
		this.id = PolyDrumBinary.count;
		this.length = length;
		this.channel = channel;
		this.midiOut = midiOut;
		this.stretch = stretch;
		if (clock == nil,
			{ this.clock = TempoClock.default},
			{ this.clock = clock} );
	}
	// sets class variable count to nil !MUST BE USED IN CLIENT
	*reset { PolyDrumBinary.count = 0; postln(["count", PolyDrumBinary.count])}

	// manages curLine and prevLine !use everytime this.curLine is updated!
	// will set both prevLine and favLine, if no former curLine
	// private! (use setNewLine in client)
	registerNewLine {|newLine|
		if (this.curLine != nil,
			{ this.prevLine = this.curLine.copy() },
			{ this.prevLine = newLine.copy(); this.favLine = newLine.copy()} );
		this.curLine = newLine.copy();
		this.curLine.postln;
	}
	// PUBLIC
	// external way to set a pattern
	setNewLine {|newLine|
		if (this.length != newLine.size,
			{ this.length = newLine.size;
			  this.curLine = nil; } ); // for registerNewLine
		this.registerNewLine(newLine);


	}

	// generate random line, forceDownBeat will make beat one play
	randomLine { |forceDownBeat = true, minSpace = 0|
		var newLine;
		this.length.do { |i|
			if (forceDownBeat == true && i == 0,
				{ newLine = newLine.add(1) },
				{ newLine = newLine.add(2.rand) } );
		};
		this.registerNewLine(newLine);
		postln(this.curLine);
	}


	minSpaceLine {|minSpace = 1, probability = 1.0|
		var newLine = [1] , gap = 0, die;
		(this.length-2).do {
			if (newLine[newLine.size-1] == 1,
				{ gap = 0 },
				{ gap = gap + 1 } );
			if (gap >= minSpace && ((newLine.size() + minSpace) < (this.length - 1)),
				{ die = 1.0.rand;
					if (die < probability, {newLine = newLine.add(1)}, {newLine = newLine.add(0)})},
				{ newLine = newLine.add(0) } );
		};
		this.registerNewLine(newLine);
	}

	// generates pattern according to euclidean algorithm, returns randomly permuted rotation
	euclid {|accents = nil|
		var front, back;
		if (accents == nil, {accents = ((this.length-1)/2).asInteger});
		front = Array.fill(accents, { [1] });
		back = Array.fill((this.length - accents), { [0] });
		this.recursive_compose(front, back);
	}

	// helper function for euclid
	recursive_compose {|front, back|
		var temp, result, first;
		// base case: second array is one (irregular) or zero (repeating)
		if (back.size <= 1,
			{   // ensure that the first element is not the back
				first = front.pop;
				result = [front ++ back].flatten;
				result = first ++ result.scramble;

				// finishes here:
				this.registerNewLine(result.flatten) },
			{
				// distribute until either array is empty
				while (
					{ (front.size > 0) && (back.size > 0) },
					{ temp = temp.add([front.pop, back.pop].flatten) }
				);
				back = front ++ back;
				this.recursive_compose(temp, back)

			};
		);
	}


	duplicate { | withSameId = false |
		var newTrack;
		if (withSameId,
			{ newTrack = this.copy(); },
			{ newTrack = PolyDrumBinary(this.length, this.channel, this.clock, this.midiOut);
				newTrack.registerNewLine(this.curLine);
				newTrack.favLine(this.favLine);
				newTrack.prevLine(this.prevLine); });
			^newTrack
	}

	// converts 0 to /rest to play in pattern
	// helper called in runPattern, essentially a wrapper for this.curLine
	playable {
		var playableLine;
		this.curLine.do { |val, i|
			if (val == 0,
				{ playableLine = playableLine.add(\rest) },
				{ playableLine = playableLine.add(1) });
		};
		^playableLine
	}

	//return to previous pattern
	previousPattern {
		if (this.prevLine != nil,
			{ this.registerNewLine(this.prevLine) }
		);
	}

	//set the current pattern as a favourite, use this.goToFav to set this to curLine
	setFav { this.favLine = this.curLine.copy() }

	// revert to favLine
	goToFav { this.registerNewLine(this.favLine) }

	//left shift arg places
	leftShift {|places = 1|
		var tempArr;
		this.curLine.do { |val, i|
			tempArr = tempArr.add(this.curLine.wrapAt(i + places));
		};
		this.registerNewLine(tempArr);
	}


	runPattern {
		// this.channel.postln;
		this.pat = Pbind (
			\type, \midi,
			\midiout, this.midiOut,
			\degree, Pn(Plazy {Pseq(this.playable)}),
			\chan, this.channel,
			\root, (this.id -1).postln,
			// \stretch, ((1/this.clock.tempo)/this.length),
			\dur, (this.clock.tempo/this.length),
			\scale, Scale.chromatic,
		).play(this.clock, quant:[1 , 0]);
	}


}
// some ideas:
// change the format to allow for beat 1 rests (use rest in patterns?)
// allow for leftSHifts of one pulse, or one unit
// use a static varianble to keep track of how many have been created, and assign midinote accordingly
// figure out why the tempoclock doesn't work
// set favourite, revert to favourite