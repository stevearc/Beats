Pkit : Pattern {
  classvar <>default;
  var <>charmap, charStreams;

  *initClass {
    default = Pkit.new;
  }

  *new {
    ^super.new.init;
  }
  init {
    charmap = IdentityDictionary.new;
    charStreams = IdentityDictionary.new;
  }
  storeArgs { ^[charmap] }

  *setDefault { |char ... pairs| Pkit.default.set(char, *pairs) }
  set { |char ... pairs|
    if (pairs.size.odd, { Error("Pkit.set should have odd number of args.\n").throw; });
    if (char.class == String) { char = char.at(0) };
    if (char.class != Char, { Error("Pkit.set first argument must be char or string.\n").throw; });
    charmap[char] = pairs;
  }

  *unsetDefault { |char| Pkit.default.unset(char) }
  unset { |char|
    charmap.removeAt(char);
  }

  embedInStream { |inevent|
    if (charmap.isEmpty and: (this != Pkit.default)) {
      // This happens if you use Pkit() instead of Pkit.default.
      // Let's just fix that for you.
      inevent = Pkit.default.embedInStream(inevent);
      ^inevent;
    };
    loop {
      var char;
      var charkit;
      var event;
      var streampairs;
      if (inevent.isNil) { ^nil };
      event = inevent.copy;
      if (event.class == Char) {
        char = event;
        event = Event();
      } {
        char = event[\char];
      };
      charkit = charmap[char];
      if (charkit.notNil) {
        streampairs = charkit.copy;
        forBy (1, streampairs.size - 1, 2) { |i|
          streampairs.put(i, streampairs[i].asStream);
        };
        forBy (0, streampairs.size - 1, 2) { |i|
          var name = streampairs[i];
          var stream = streampairs[i+1];
          var streamout = stream.next(event);
          if (streamout.isNil) { ^nil };

          if (name.isSequenceableCollection) {
            if (name.size > streamout.size) {
              ("the pattern is not providing enough values to assign to the key set:" + name).warn;
              ^nil
            };
            name.do { arg key, i;
              event.put(key, streamout[i]);
            };
          }{
            event.put(name, streamout);
          };
        };
      };
      // TODO FIXME we'll always just use the first value from these patterns???
      inevent = event.yield;
    }
  }
}

Pbindkit : Pattern {
  var <>pattern, <>basedur, <>kit, <patternpairs;

  *new { | pattern ... pairs|
    var basedur = 1;
    var kit = Pkit.default;
    var removeIndexes = [];
    if (pattern.size == 0, { Error("Pbindkit pattern cannot be empty.\n").throw; });
    if (pairs.size.odd, { Error("Pbindkit should have odd number of args.\n").throw; });
    forBy (0, pairs.size - 1, 2) { |i|
      var name = pairs[i];
      var val = pairs[i+1];
      if (name == \dur) {
        basedur = val;
        removeIndexes = removeIndexes.add(i);
      };
      if (name == \kit) {
        kit = val;
        removeIndexes = removeIndexes.add(i);
      };
    };
    removeIndexes.sort({ |a, b| a > b });
    removeIndexes.do { |i|
      pairs.removeAt(i + 1);
      pairs.removeAt(i);
    }
    ^super.newCopyArgs(pattern, basedur, kit, pairs)
  }
  storeArgs { ^[pattern, basedur, kit, patternpairs] }

  embedInStream { |inevent|
    var p = Pbind(*patternpairs) <> kit <> Pbeat(pattern, basedur: basedur);
    p.embedInStream(inevent);
  }
}
