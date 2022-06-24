package edu.cornell.cs.apl.viaduct.algebra

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class FreeDistributiveLatticeTest {
    private val elemA = FreeDistributiveLattice("a")
    private val elemB = FreeDistributiveLattice("b")
    private val elemC = FreeDistributiveLattice("c")
    private val top = FreeDistributiveLattice.bounds<String>().top
    private val bottom = FreeDistributiveLattice.bounds<String>().bottom

    @Test
    fun testImply() {
        /* For all y, the greatest x s.t. bottom & x <= y is top. */
        Assertions.assertEquals(top, bottom.imply(elemA))

        /* For all y, the greatest x s.t. y & x <= y is top. */
        Assertions.assertEquals(top, elemA.imply(elemA))
        Assertions.assertEquals(elemB, elemA.imply(elemB))
        Assertions.assertEquals(elemB, elemA.imply(elemB.meet(elemA)))
        Assertions.assertEquals(top, elemB.join(elemA).imply(elemA.join(elemB)))
        Assertions.assertEquals(elemA, elemA.join(elemB).imply(elemA))
        Assertions.assertEquals(top, elemA.meet(elemB).imply(elemA))
        Assertions.assertEquals(top, elemA.imply(top))
        Assertions.assertEquals(bottom, elemA.imply(bottom))
        Assertions.assertEquals(top, elemA.imply(elemA.join(elemB)))
        Assertions.assertEquals(elemA.imply(elemB).meet(elemA.imply(elemC)), elemA.imply(elemB.meet(elemC)))
        Assertions.assertEquals(elemA.meet(elemB), elemA.meet(elemA.imply(elemB)))
        Assertions.assertEquals(elemB, elemB.meet(elemA.imply(elemB)))
    }
}
