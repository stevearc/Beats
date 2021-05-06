Pbeat : Pattern {
  var <>pattern, <>basedur, <>repeat, repeatIndex = 0;

  *new { | pattern, repeat = inf, basedur = 1.0 |
    if (pattern.size == 0, { Error("Pbeat pattern cannot be empty.\n").throw; });
    ^super.newCopyArgs(BeatStream(pattern), basedur, repeat)
  }

  printOn { |stream|
    this.storeOn(stream);
  }
  storeOn { |stream|
    stream << "Pbeat(\"" << pattern << "\", " <<< basedur <<< ", " <<< repeat << ")";
  }

  embedInStream { |inevent|
    var event;
    var context = BeatStreamContext();
    this.reset;
    loop {
      var char;
      if (inevent.isNil) { ^nil.yield };
      char = pattern.iterateNext(context);
      if (char.isNil) {
        repeatIndex = repeatIndex + 1;
        if (repeatIndex < repeat) {
          context = BeatStreamContext();
          pattern.reset;
          char = pattern.iterateNext(context);
        } {
          ^inevent;
        };
      };
      event = inevent.copy;
      event.put(\char, char);
      if (char == Char.space)
        { event.put(\dur, Rest(basedur * context.durationModifier)); }
        { event.put(\dur, basedur * context.durationModifier); };

      inevent = event.yield;
    }
  }

  reset {
    pattern.reset;
    repeatIndex = 0;
  }
}
