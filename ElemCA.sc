ElemCA : BasicCA {

	var <>width, <>rules, <>keys, <>ruleNum;

	*new {|width, rules, firstState, midiout|
		^super.new.init(width, rules, firstState,midiout)

	}

	init {|width, rules, firstState, midiout|
		super.init(width, rules, firstState,midiout);
		this.keys = ["111","110","101","100","011","010","001","000"];
		this.rules = this.createRules(rules);

	}


	createRules {|rules|
		var rulesDict = Dictionary.new;
		if (rules.isString,
			{ rulesDict = this.makeRulesFromString(rules)},
			{ rulesDict = this.makeRulesFromInt(rules)});
		^rulesDict;
	}

	makeRulesFromString { |rules|
		var rulesDict = Dictionary.new();
		if (rules.size != 8, { postln("rule string needs 8 ones or zeros"); ^nil; });
		rules.do { |rule, i|
			rule = rule.asString;
			if ((rule != "1") && (rule != "0"), {postln(["rules must contain only 0 and 1:", rule])});
			rulesDict.add(this.keys[i] -> rule);
		};
		this.setRuleNumFromString(rulesDict);
		^rulesDict;
	}

	setRuleNumFromString { |rulesDict|
		var reverseKeys, total = 0;
		reverseKeys = this.keys.reverse();
		reverseKeys.do { |key, i|
			total = total + (rulesDict[key.asString].asInteger*(2**i));
		};
		this.ruleNum = total;
	}
	makeRulesFromInt { |rules|
		var ruleArray, rulesDict = Dictionary.new();
		if (rules.isInteger != true, {"must be integer (or string)".postln; ^nil});
		if ((rules < 0) || (rules > 255), {"rule number out of range".postln; ^nil});
		ruleArray = rules.asBinaryDigits;
		ruleArray.do { |rule, i|
			rulesDict.add(this.keys[i] -> rule.asString);
		};
		this.ruleNum = rules;
		^rulesDict;

	}
	showRules {
		this.keys.do {|key| [key, this.rules[key]].postln};
	}

	getNext {
		var nextGen;
	/*	if (this.started,*/
		this.prevState = this.nextState.copy;
				this.history = this.history.add(this.prevState);
		this.prevState.do { |state, i|
			var neighbours =  this.prevState.wrapAt(i-1).asString ++ state.asString ++ this.prevState.wrapAt(i+1).asString;
			nextGen = nextGen ++ this.rules[neighbours];
		};
		this.nextState = nextGen.copy;
		// this.started = true;
		^nextGen;
	}
}