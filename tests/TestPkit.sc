TestPkit : UnitTest {
  var kit;

  setUp {
    kit = Pkit();
  }

  test_add_key {
    var p = Pbindkit("o", \kit, kit);
    var event;
    kit.set($o, \foo, "bar");

    event = p.asStream.next(());
    this.assertEquals(event[\char], $o);
    this.assertEquals(event[\foo], "bar");
  }

  test_pattern {
    var p = Pbindkit("o", \kit, kit);
    var event;
    kit.set($o, \foo, Pseq([1,2,3]));

    event = p.asStream.next(());
    this.assertEquals(event[\foo], 1);
    event = p.asStream.next(());
    this.assertEquals(event[\foo], 2);
    event = p.asStream.next(());
    this.assertEquals(event[\foo], 3);
    // Non-infinite patterns should loop
    event = p.asStream.next(());
    this.assertEquals(event[\foo], 1);
  }
}

