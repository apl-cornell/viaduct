package io.github.apl_cornell.viaduct.algebra

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class FreeDistributiveLatticeTest {
    private val a = FreeDistributiveLattice("a")
    private val b = FreeDistributiveLattice("b")
    private val c = FreeDistributiveLattice("c")
    private val top = FreeDistributiveLattice.bounds<String>().top
    private val bottom = FreeDistributiveLattice.bounds<String>().bottom

    @Test
    fun imply() {
        /* For all y, the greatest x s.t. bottom & x <= y is top. */
        Assertions.assertEquals(top, bottom.imply(a))

        /* For all y, the greatest x s.t. y & x <= y is top. */
        Assertions.assertEquals(top, a.imply(a))
        Assertions.assertEquals(b, a.imply(b))
        Assertions.assertEquals(b, a.imply(b.meet(a)))
        Assertions.assertEquals(top, b.join(a).imply(a.join(b)))
        Assertions.assertEquals(a, a.join(b).imply(a))
        Assertions.assertEquals(top, a.meet(b).imply(a))
        Assertions.assertEquals(top, a.imply(top))
        Assertions.assertEquals(bottom, a.imply(bottom))
        Assertions.assertEquals(top, a.imply(a.join(b)))
        Assertions.assertEquals(a.imply(b).meet(a.imply(c)), a.imply(b.meet(c)))
        Assertions.assertEquals(a.meet(b), a.meet(a.imply(b)))
        Assertions.assertEquals(b, b.meet(a.imply(b)))
    }

    @Nested
    inner class Assumptions {
        @Test
        fun simple() {
            assertFlowsTo(a, b, a to b)
            assertFlowsTo(a, c, a to b, b to c)
            assertFlowsTo(a, c, b to c, a to b)
        }

        @Test
        fun `join left`() {
            assertFlowsTo(a join b, c, a to c, b to c)
            assertFlowsTo(a join b, c, (a join b) to c)
        }

        @Test
        fun `join right`() {
            assertFlowsTo(a, b join c, a to b)
            assertFlowsTo(a, b join c, a to (b join c))
        }

        @Test
        fun `meet left`() {
            assertFlowsTo(a meet b, c, a to c)
            assertFlowsTo(a meet b, c, (a meet b) to c)
        }

        @Test
        fun `meet right`() {
            assertFlowsTo(a, b meet c, a to b, a to c)
            assertFlowsTo(a, b meet c, a to (b meet c))
        }
    }

    companion object {
        private fun <A> assertFlowsTo(
            from: FreeDistributiveLattice<A>,
            to: FreeDistributiveLattice<A>,
            vararg assumptions: Pair<FreeDistributiveLattice<A>, FreeDistributiveLattice<A>>
        ) {
            val assumptionsList = assumptions.map { FreeDistributiveLattice.LessThanOrEqualTo(it.first, it.second) }
            Assertions.assertFalse(from.lessThanOrEqualTo(to))
            Assertions.assertTrue(from.lessThanOrEqualTo(to, assumptionsList))
        }
    }
}
