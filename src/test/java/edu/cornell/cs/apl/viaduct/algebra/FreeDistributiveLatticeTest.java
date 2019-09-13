package edu.cornell.cs.apl.viaduct.algebra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FreeDistributiveLatticeTest {
  private final FreeDistributiveLattice<String> elemA = FreeDistributiveLattice.create("a");
  private final FreeDistributiveLattice<String> elemB = FreeDistributiveLattice.create("b");
  private final FreeDistributiveLattice<String> elemC = FreeDistributiveLattice.create("c");
  private final FreeDistributiveLattice<String> top = FreeDistributiveLattice.top();
  private final FreeDistributiveLattice<String> bottom = FreeDistributiveLattice.bottom();

  @Test
  void testImply() {
    /* for all y, the greatest x s.t. bottom & x <= y is top */
    assertEquals(bottom.imply(elemA), top);

    /* for all y, the greatest x s.t. y & x <= y is top */
    assertEquals(elemA.imply(elemA), top);

    assertEquals(elemA.imply(elemB), elemB);

    assertEquals(elemA.imply(elemB.meet(elemA)), elemB);

    assertEquals(elemB.join(elemA).imply(elemA.join(elemB)), top);

    assertEquals(elemA.join(elemB).imply(elemA), elemA);

    assertEquals(elemA.meet(elemB).imply(elemA), top);

    assertEquals(elemA.imply(top), top);

    assertEquals(elemA.imply(bottom), bottom);

    assertEquals(elemA.imply(elemA.join(elemB)), top);

    assertEquals(elemA.imply(elemB.meet(elemC)), elemA.imply(elemB).meet(elemA.imply(elemC)));

    assertEquals(elemA.meet(elemA.imply(elemB)), elemA.meet(elemB));

    assertEquals(elemB.meet(elemA.imply(elemB)), elemB);
  }
}
