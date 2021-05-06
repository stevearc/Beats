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
      [Pbeat("x", 1), "x"],
      [Pbeat("xoxo", 1), "xoxo"],
      [Pbeat("x[xx]", 1), "xxx", [1, 0.5, 0.5]],
    ];
    cases.do { |c|
      this.prAssertStreamEquals(*c);
    }
  }
}
