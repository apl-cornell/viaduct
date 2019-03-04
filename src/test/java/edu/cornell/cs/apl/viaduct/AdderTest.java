package edu.cornell.cs.apl.viaduct;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AdderTest {

  @Test
  public void testZeroIsLeftIdentityForAdd() {
    final Adder adder = new Adder();
    assertEquals("0 + 4 should equal 4", 4, adder.add(0, 4));
  }

  @Test
  public void testZeroIsRightIdentityForAdd() {
    final Adder adder = new Adder();
    assertEquals("4 + 0 should equal 4", 4, adder.add(4, 0));
  }
}
