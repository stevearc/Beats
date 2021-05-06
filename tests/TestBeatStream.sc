TestBeatStream : UnitTest {

  prGetArrayAndDurations { |stream|
    var arr = Array();
    var durs = Array();
    var context = BeatStreamContext();
    var val = stream.iterateNext(context);
    while { val.notNil } {
      arr = arr.add(val);
      durs = durs.add(context.durationModifier);
      context.reset;
      val = stream.iterateNext(context);
    };

    ^[arr, durs];
  }

  prAssertStreamEquals { |pattern, elements, durations|
    var ret = this.prGetArrayAndDurations(BeatStream(pattern));
    if (elements.class == String) {
      var scratch = Array();
      elements.size.do { |i|
        scratch = scratch.add(elements.at(i));
      };
      elements = scratch;
    };
    durations = durations.collect(_.asFloat);
    this.assert(ret[0] == elements, "Pattern elements \"" ++ pattern ++ "\"\n\tExpected: " ++ elements ++ "\n\tReceived: " ++ ret[0]);
    this.assert(ret[1] == durations, "Pattern durations \"" ++ pattern ++ "\"\n\tExpected: " ++ durations ++ "\n\tReceived: " ++ ret[1]);
  }

  test_patterns {
    this.prAssertStreamEquals("xoxo", "xoxo", 1!4);
    this.prAssertStreamEquals("x[--]x", "x--x", [1, 0.5, 0.5, 1]);
    this.prAssertStreamEquals(
      "x[-[--]]x",
      "x---x",
      [1, 0.5, 0.25, 0.25, 1]
    );
    this.prAssertStreamEquals("<xo>", "xo", [0, 1]);
    this.prAssertStreamEquals(
      "o<-x>o<-x>",
      "o-xo-x",
      [1, 0, 1, 1, 0, 1]
    );

    this.prAssertStreamEquals("xo(xo)", "xoxxoo", 1!6);
    this.prAssertStreamEquals("xo(x,o)", "xoxxoo", 1!6);
    this.prAssertStreamEquals("--(x,o)--", "--x----o--", 1!10);
    this.prAssertStreamEquals("-(xo)-(yz)-", "-x-y--o-z-", 1!10);
    this.prAssertStreamEquals("-(xo,y(aa,bb))z", "-xoz-yaaz-ybbz", 1!14);
    this.prAssertStreamEquals("-(x[-x])o", "-xo--xo", [1, 1, 1, 1, 0.5, 0.5, 1]);
  }

  test_random_patterns {
    thisThread.randSeed = 1;
    this.prAssertStreamEquals("{xo}", "o", [1]);
    thisThread.randSeed = 5;
    this.prAssertStreamEquals("{xo}", "x", [1]);
    thisThread.randSeed = 9101;
    this.prAssertStreamEquals("{(x,y)o}", "xy", 1!2);
  }

}
