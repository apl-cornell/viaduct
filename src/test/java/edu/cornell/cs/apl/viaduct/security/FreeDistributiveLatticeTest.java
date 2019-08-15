package edu.cornell.cs.apl.viaduct.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FreeDistributiveLatticeTest {
  private final FreeDistributiveLattice<String> elemA = new FreeDistributiveLattice<String>("a");
  private final FreeDistributiveLattice<String> elemB = new FreeDistributiveLattice<String>("b");
  private final FreeDistributiveLattice<String> top = FreeDistributiveLattice.top();
  private final FreeDistributiveLattice<String> bottom = FreeDistributiveLattice.bottom();

  @Test
  void testRelativePseudocomplement1() {
    /* for all y, the greatest x s.t. bottom & x <= y is top */
    assertEquals(elemA.relativePseudocomplement(bottom), top);
  }

  @Test
  void testRelativePseudocomplement2() {
    /* for all y, the greatest x s.t. y & x <= y is top */
    assertEquals(elemA.relativePseudocomplement(elemA), top);
  }

  @Test
  void testRelativePseudocomplement3() {
    assertEquals(elemB.relativePseudocomplement(elemA), elemB);
  }

  @Test
  void testRelativePseudocomplement4() {
    assertEquals(elemB.meet(elemA).relativePseudocomplement(elemA), elemB);
  }

  @Test
  void testRelativePseudocomplement5() {
    assertEquals(elemB.join(elemA).relativePseudocomplement(elemA.join(elemB)), top);
  }

  @Test
  void testRelativePseudocomplement6() {
    assertEquals(elemA.relativePseudocomplement(elemA.join(elemB)), elemA);
  }

  @Test
  void testRelativePseudocomplement7() {
    assertEquals(elemA.relativePseudocomplement(elemA.meet(elemB)), top);
  }

  @Test
  void testRelativePseudocomplement8() {
    assertEquals(top.relativePseudocomplement(elemA), top);
  }

  @Test
  void testRelativePseudocomplement9() {
    assertEquals(bottom.relativePseudocomplement(elemA), bottom);
  }

  @Test
  void testRelativePseudocomplement10() {
    assertEquals(elemA.join(elemB).relativePseudocomplement(elemA), top);
  }
}
