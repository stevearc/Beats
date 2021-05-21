BeatStreamContext {
  var <>durationModifier = 1.0;
  var <iterationLevel = 0;
  var iterationByLevel;
  var highestLevelWithMoreIterations = -1;

  *new {
    ^super.new.init;
  }

  init {
    iterationByLevel = [0];
  }

  iteration {
    ^(iterationByLevel[iterationLevel]);
  }

  hasMoreIterations {
    ^(highestLevelWithMoreIterations >= 0);
  }

  markHasMoreIterations {
    if (iterationLevel > highestLevelWithMoreIterations) {
      highestLevelWithMoreIterations = iterationLevel;
    };
  }

  incrementIteration {
    if (highestLevelWithMoreIterations == -1) { Error("Cannot increment. No more iteration levels.").throw };
    iterationByLevel[highestLevelWithMoreIterations] = iterationByLevel[highestLevelWithMoreIterations] + 1;
    if (highestLevelWithMoreIterations < (iterationByLevel.size - 1)) {
      for((highestLevelWithMoreIterations + 1), (iterationByLevel.size - 1), { |i|
        iterationByLevel[i] = 0;
      });
    };
    highestLevelWithMoreIterations = -1;
  }

  increaseLevel {
    iterationLevel = iterationLevel + 1;
    if (iterationLevel > (iterationByLevel.size - 1)) {
      iterationByLevel = iterationByLevel.add(0);
    }
  }

  decreaseLevel {
    iterationLevel = iterationLevel - 1;
  }

  reset {
    durationModifier = 1.0;
  }

  printOn { |stream|
    stream << "Context(\n\titerationByLevel: " << iterationByLevel
      << "\n\tdur: " << durationModifier
      << "\n\thighestLevelWithMoreIterations: " << highestLevelWithMoreIterations
      << "\n\tlevel: " << iterationLevel
      << "\n)";
  }
}

BeatStream : Stream {
  var elements, index = 0;

  *new { | pattern |
    var ret = BeatStream.parse(pattern);
    ^super.new.init(ret[0]);
  }

  init { |e|
    elements = e;
  }

  *parse { | pattern, pointer = 0, endchar = nil, endchar2 = nil |
    var elements = Array();
    var char = pattern.at(pointer);
    var element;
    while { pointer < pattern.size } {
      var ret;
      pointer = pointer + 1;
      switch(char,
        $[, {
          ret = BeatStream.parse(pattern, pointer, $]);
          pointer = ret[1];
          elements = elements.add(BeatScaledStream(ret[0]));
        },
        $<, {
          ret = BeatStream.parse(pattern, pointer, $>);
          pointer = ret[1];
          elements = elements.add(BeatParallelStream(ret[0]));
        },
        ${, {
          ret = BeatStream.parse(pattern, pointer, $});
          pointer = ret[1];
          elements = elements.add(BeatRandomStream(ret[0]));
        },
        $(, {
          ret = BeatRepeatStream.parse(pattern, pointer);
          pointer = ret[1];
          elements = elements.add(BeatRepeatStream(ret[0]));
        },
        {endchar}, { ^[elements, pointer] },
        {endchar2}, { ^[elements, pointer] },
        { elements = elements.add(char) },
      );
      char = pattern.at(pointer);
    }

    ^[elements, pointer]
  }

  *nextFromList { |list, idx, context|
    var element = list[idx];
    var value = nil;
    if (element.isNil) { ^[nil, idx] };
    value = element.next(context);
    while { value.isNil && (idx < (list.size - 1)) } {
      idx = idx + 1;
      element = list[idx];
      value = element.next(context);
    };
    if (element.class == Char) {
      idx = idx + 1;
    };
    ^[value, idx];
  }

  iterateNext { |context|
    var char;
    context.reset;
    char = this.next(context);
    while {char.isNil} {
      if (context.hasMoreIterations) {
        // If we've finished an iteration, increment & try again
        context.incrementIteration;
        context.reset;
        this.reset(true);
      } {
        ^nil;
      };
      char = this.next(context);
    };
    ^char;
  }

  next { |context|
    var ret = BeatStream.nextFromList(elements, index, context);
    index = ret[1];
    ^ret[0];
  }

  reset { |soft|
    index = 0;
    elements.do { |element|
      element.reset(soft);
    }
  }

  printOn { |stream|
    elements.do { |e|
      stream << e;
    }
  }

  storeOn { |stream|
    stream << "BeatStream(\"";
    elements.do { |e|
      stream << e;
    };
    stream << "\")";
  }
}

BeatScaledStream {
  var elements, index = 0;

  *new { |e|
    ^super.new.init(e);
  }

  init { |e|
    elements = e;
  }

  next { |context|
    var ret;
    ret = BeatStream.nextFromList(elements, index, context);
    if (context.notNil && ret[0].notNil) {
      context.durationModifier = context.durationModifier / elements.size;
    };
    index = ret[1];
    ^ret[0];
  }

  reset { |soft|
    index = 0;
    elements.do { |element|
      element.reset(soft);
    }
  }

  printOn { |stream|
    stream << "[";
    elements.do { |e|
      stream << e;
    };
    stream << "]";
  }

  storeOn { |stream|
    stream << "BeatStream(\"";
    stream << this;
    stream << "\")";
  }
}

BeatParallelStream {
  var elements, index = 0;

  *new { |e|
    ^super.new.init(e);
  }

  init { |e|
    elements = e;
  }

  next { |context|
    var ret = BeatStream.nextFromList(elements, index, context);
    index = ret[1];
    if (context.notNil && (index < elements.size)) {
      context.durationModifier = 0;
    };
    ^ret[0];
  }

  reset {
    index = 0;
  }

  printOn { |stream|
    stream << "<";
    elements.do { |e|
      stream << e;
    };
    stream << ">";
  }

  storeOn { |stream|
    stream << "BeatStream(\"";
      stream << this;
      stream << "\")";
    }
  }

BeatRandomStream {
  var options, index;

  *new { |e|
    ^super.new.init(e);
  }

  init { |e|
    options = e;
    index = rrand(0, options.size - 1);
  }

  next { |context|
    var val = nil;
    if (index > -1) {
      val = options[index].next(context);
      if (options[index].class == Char) {
        index = -1;
      };
    };
    ^val;
  }

  printOn { |stream|
    stream << "{";
    options.do { |e|
      stream << e;
    };
    stream << "}";
  }

  storeOn { |stream|
    stream << "BeatStream(\"";
    stream << this;
    stream << "\")";
  }

  reset { |soft|
    if (soft.not) {
      index = rrand(0, options.size - 1);
    };
    options.do { |element|
      element.reset(soft);
    }
  }
}

BeatRepeatStream {
  var paths, index = 0;

  *new { |e|
    ^super.new.init(e);
  }

  *parse { | pattern, pointer = 0 |
    var paths = Array();
    while { pointer < pattern.size && (pattern.at(pointer - 1) != $)) } {
      var ret = BeatStream.parse(pattern, pointer, $), $,);
      pointer = ret[1];
      paths = paths.add(ret[0]);
    };

    // If no commas inside (), treat each item as a separate path
    if (paths.size == 1) {
      paths = paths[0].collect({|e, i| [e]});
    }

    ^[paths, pointer]
  }

  init { |e|
    paths = e;
  }

  next { |context|
    var path = paths.wrapAt(context.iteration);
    var ret;
    if (context.iteration < (paths.size - 1)) {
      context.markHasMoreIterations
    };
    context.increaseLevel;
    ret = BeatStream.nextFromList(path, index, context);
    context.decreaseLevel;
    index = ret[1];
    ^ret[0];
  }

  printOn { |stream|
    stream << "(";
    paths.do { |path, i|
      path.do { |e|
        stream << e;
      };
      if (i < (paths.size - 1)) {
        stream << ",";
      };
    };
    stream << ")";
  }

  storeOn { |stream|
    stream << "BeatStream(\"";
    stream << this;
    stream << "\")";
  }

  reset { |soft|
    index = 0;
    paths.do { |path|
      path.do { |element|
        element.reset(soft);
      }
    }
  }
}
