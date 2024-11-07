package io.github.aplcornell.viaduct.algebra

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
        // For all y, the greatest x s.t. bottom & x <= y is top.
        Assertions.assertEquals(top, bottom.imply(elemA))

        // For all y, the greatest x s.t. y & x <= y is top.
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

    @Test
    fun testLEQ() {
        val context0 = listOf<FreeDistributiveLattice.LessThanOrEqualTo<String>>()
        val context1 = listOf(FreeDistributiveLattice.LessThanOrEqualTo(elemA, elemB))
        val context2 = listOf(FreeDistributiveLattice.LessThanOrEqualTo(elemA, top))
        val context3 = listOf(FreeDistributiveLattice.LessThanOrEqualTo(elemA, bottom))
        val context4 =
            listOf(
                FreeDistributiveLattice.LessThanOrEqualTo(elemA, elemA.meet(elemB.join(elemC))),
                FreeDistributiveLattice.LessThanOrEqualTo(elemC.meet(elemA.join(elemB)), elemB.meet(elemC)),
            )

        assert(elemA.lessThanOrEqualTo(elemA, context0))
        assert(top.lessThanOrEqualTo(top, context0))
        assert(bottom.lessThanOrEqualTo(bottom, context0))
        assert(elemA.lessThanOrEqualTo(top, context0))
        assert(elemA.join(elemB).lessThanOrEqualTo(top, context0))
        assert(elemA.meet(elemB).lessThanOrEqualTo(top, context0))
        assert(bottom.lessThanOrEqualTo(elemA, context0))
        assert(bottom.lessThanOrEqualTo(elemA.join(elemB), context0))
        assert(bottom.lessThanOrEqualTo(elemA.meet(elemB), context0))
        assert(elemA.lessThanOrEqualTo(elemA.join(elemB), context0))
        assert(elemA.meet(elemB).lessThanOrEqualTo(elemA, context0))

        assert(elemA.lessThanOrEqualTo(elemA, context2))
        assert(top.lessThanOrEqualTo(top, context2))
        assert(bottom.lessThanOrEqualTo(bottom, context2))
        assert(elemA.lessThanOrEqualTo(top, context2))
        assert(elemA.join(elemB).lessThanOrEqualTo(top, context2))
        assert(elemA.meet(elemB).lessThanOrEqualTo(top, context2))
        assert(bottom.lessThanOrEqualTo(elemA, context2))
        assert(bottom.lessThanOrEqualTo(elemA.join(elemB), context2))
        assert(bottom.lessThanOrEqualTo(elemA.meet(elemB), context2))
        assert(elemA.lessThanOrEqualTo(elemA.join(elemB), context2))
        assert(elemA.meet(elemB).lessThanOrEqualTo(elemA, context2))

        assert(elemA.lessThanOrEqualTo(elemA, context1))
        assert(top.lessThanOrEqualTo(top, context1))
        assert(bottom.lessThanOrEqualTo(bottom, context1))
        assert(elemA.lessThanOrEqualTo(top, context1))
        assert(elemA.join(elemB).lessThanOrEqualTo(top, context1))
        assert(elemA.meet(elemB).lessThanOrEqualTo(top, context1))
        assert(bottom.lessThanOrEqualTo(elemA, context1))
        assert(bottom.lessThanOrEqualTo(elemA.join(elemB), context1))
        assert(bottom.lessThanOrEqualTo(elemA.meet(elemB), context1))
        assert(elemA.lessThanOrEqualTo(elemA.join(elemB), context1))
        assert(elemA.join(elemB).lessThanOrEqualTo(elemB, context1))
        assert(elemA.meet(elemB).lessThanOrEqualTo(elemA, context1))
        assert(elemA.lessThanOrEqualTo(elemA.meet(elemB), context1))
        assert(elemA.join(elemB.meet(elemC)).lessThanOrEqualTo(elemB.meet(elemA.join(elemC)), context1))

        assert(elemA.lessThanOrEqualTo(elemA, context4))
        assert(top.lessThanOrEqualTo(top, context4))
        assert(bottom.lessThanOrEqualTo(bottom, context4))
        assert(elemA.lessThanOrEqualTo(top, context4))
        assert(elemA.join(elemB).lessThanOrEqualTo(top, context4))
        assert(elemA.meet(elemB).lessThanOrEqualTo(top, context4))
        assert(bottom.lessThanOrEqualTo(elemA, context4))
        assert(bottom.lessThanOrEqualTo(elemA.join(elemB), context4))
        assert(bottom.lessThanOrEqualTo(elemA.meet(elemB), context4))
        assert(elemA.lessThanOrEqualTo(elemA.join(elemB), context4))
        assert(elemA.join(elemB).lessThanOrEqualTo(elemB, context4))
        assert(elemA.meet(elemB).lessThanOrEqualTo(elemA, context4))
        assert(elemA.lessThanOrEqualTo(elemA.meet(elemB), context4))
        assert(elemA.join(elemB.meet(elemC)).lessThanOrEqualTo(elemB.meet(elemA.join(elemC)), context4))

        assert(elemA.lessThanOrEqualTo(elemA, context3))
        assert(top.lessThanOrEqualTo(top, context3))
        assert(bottom.lessThanOrEqualTo(bottom, context3))
        assert(elemA.lessThanOrEqualTo(top, context3))
        assert(elemA.join(elemB).lessThanOrEqualTo(top, context3))
        assert(elemA.meet(elemB).lessThanOrEqualTo(top, context3))
        assert(bottom.lessThanOrEqualTo(elemA, context3))
        assert(bottom.lessThanOrEqualTo(elemA.join(elemB), context3))
        assert(bottom.lessThanOrEqualTo(elemA.meet(elemB), context3))
        assert(elemA.lessThanOrEqualTo(elemA.join(elemB), context3))
        assert(elemA.meet(elemB).lessThanOrEqualTo(elemA, context3))
        assert(elemA.lessThanOrEqualTo(bottom, context3))
        assert(elemA.meet(elemB).lessThanOrEqualTo(bottom, context3))
    }
}
