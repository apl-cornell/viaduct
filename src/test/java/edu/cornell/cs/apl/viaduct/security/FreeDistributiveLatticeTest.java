package edu.cornell.cs.apl.viaduct.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FreeDistributiveLatticeTest {
  private final FreeDistributiveLattice<String> elemA = new FreeDistributiveLattice<String>("a");
  private final FreeDistributiveLattice<String> elemB = new FreeDistributiveLattice<String>("b");
  private final FreeDistributiveLattice<String> elemC = new FreeDistributiveLattice<String>("c");
  private final FreeDistributiveLattice<String> top = FreeDistributiveLattice.top();
  private final FreeDistributiveLattice<String> bottom = FreeDistributiveLattice.bottom();

  @Test
  void testRelativePseudocomplement1() {
    /* for all y, the greatest x s.t. bottom & x <= y is top */
    assertEquals(bottom.relativePseudocomplement(elemA), top);
  }

  @Test
  void testRelativePseudocomplement2() {
    /* for all y, the greatest x s.t. y & x <= y is top */
    assertEquals(elemA.relativePseudocomplement(elemA), top);
  }

  @Test
  void testRelativePseudocomplement3() {
    assertEquals(elemA.relativePseudocomplement(elemB), elemB);
  }

  @Test
  void testRelativePseudocomplement4() {
    assertEquals(elemA.relativePseudocomplement(elemB.meet(elemA)), elemB);
  }

  @Test
  void testRelativePseudocomplement5() {
    assertEquals(elemB.join(elemA).relativePseudocomplement(elemA.join(elemB)), top);
  }

  @Test
  void testRelativePseudocomplement6() {
    assertEquals(elemA.join(elemB).relativePseudocomplement(elemA), elemA);
  }

  @Test
  void testRelativePseudocomplement7() {
    assertEquals(elemA.meet(elemB).relativePseudocomplement(elemA), top);
  }

  @Test
  void testRelativePseudocomplement8() {
    assertEquals(elemA.relativePseudocomplement(top), top);
  }

  @Test
  void testRelativePseudocomplement9() {
    assertEquals(elemA.relativePseudocomplement(bottom), bottom);
  }

  @Test
  void testRelativePseudocomplement10() {
    assertEquals(elemA.relativePseudocomplement(elemA.join(elemB)), top);
  }

  @Test
  void testRelativePseudocomplement11() {
    assertEquals(
          elemA.relativePseudocomplement(elemB.meet(elemC)),
          elemA.relativePseudocomplement(elemB).meet(elemA.relativePseudocomplement(elemC))
    );
  }

  @Test
  void testRelativePseudocomplement12() {
    assertEquals(
        elemA.meet(elemA.relativePseudocomplement(elemB)),
        elemA.meet(elemB)
    );
  }

  @Test
  void testRelativePseudocomplement13() {
    assertEquals(
        elemB.meet(elemA.relativePseudocomplement(elemB)),
        elemB
    );
  }
}
