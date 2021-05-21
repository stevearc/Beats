TestPbeat : UnitTest {

  prAssertStreamEquals { |beat, expected, durs = nil|
    var received = beat.asStream.all(()).collect(_[\char]).join("");
    var receivedDurs;
    if (expected.class != String) {
      expected = expected.join("");
    };

    this.assert(received == expected,
      "Beat stream \"" ++ beat
      ++ "\"\n\tExpected: " ++ expected
      ++ "\n\tReceived: " ++ received);

    if (durs.notNil) {
      receivedDurs = beat.asStream.all(()).collect(_[\dur]);
      this.assert(receivedDurs == durs,
        "Beat dur stream \"" ++ beat
        ++ "\"\n\tExpected: " ++ durs.collect(_.asFloat)
        ++ "\n\tReceived: " ++ receivedDurs);
    }
  }

  test_dup {
    var cases = [
      [Pbeat("x",1), "x"],
      [Pbeat("xoxo",1), "xoxo"],
      [Pbeat("xo o",1,0.5), "xo o", [0.5,0.5,Rest(0.5),0.5]],
      [Pbeat("x[xx]",1), "xxx", [1, 0.5, 0.5]],
    ];
    cases.do { |c|
      this.prAssertStreamEquals(*c);
    }
  }

  test_beatdur {
    var cases = [
      [Pbeatdur("xoxo",1), [1,1,1,1]],
      [Pbeatdur("x[xx] o",1), [1,0.5,0.5,Rest(1),1]],
    ];
    cases.do { |c|
      var received = c[0].asStream.all(());
      this.assert(c[1] == received,
        "Pattern durations \"" ++ c[0]
        ++ "\"\n\tExpected: " ++ c[1]
        ++ "\n\tReceived: " ++ received);
    };
  }
}
