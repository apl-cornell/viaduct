package io.github.aplcornell.viaduct.security

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private typealias Component = FreeDistributiveLattice<String>

private val ComponentBounds = Component.bounds<String>()

/** Shorthand for creating elements. */
private fun e(principal: String) = SecurityLattice(Component(principal))

/** Shorthand for projecting confidentiality. */
private val SecurityLattice<Component>.c
    get() = this.confidentiality(ComponentBounds)

/** Shorthand for projecting integrity. */
private val SecurityLattice<Component>.i
    get() = this.integrity(ComponentBounds)

private fun assertActsFor(from: SecurityLattice<Component>, to: SecurityLattice<Component>) {
    val context = listOf<FreeDistributiveLattice.LessThanOrEqualTo<String>>()
    assertTrue(from.integrityComponent.lessThanOrEqualTo(to.integrityComponent, context))
    assertTrue(from.confidentialityComponent.lessThanOrEqualTo(to.confidentialityComponent, context))
}

private fun assertFlowsTo(from: SecurityLattice<Component>, to: SecurityLattice<Component>) {
    assertActsFor(
        to.confidentiality(FreeDistributiveLattice.bounds()),
        from.confidentiality(FreeDistributiveLattice.bounds()),
    )
    assertActsFor(
        from.integrity(FreeDistributiveLattice.bounds()),
        to.integrity(FreeDistributiveLattice.bounds()),
    )
}

internal class SecurityLatticeTest {
    @Test
    fun `confidentiality projection`() {
        assertEquals(Component("A"), e("A").c.confidentialityComponent)
        assertEquals(ComponentBounds.top, e("A").c.integrityComponent)
    }

    @Test
    fun `integrity projection`() {
        assertEquals(ComponentBounds.top, e("A").i.confidentialityComponent)
        assertEquals(Component("A"), e("A").i.integrityComponent)
    }

    @Test
    fun join() {
        assertFlowsTo(e("A"), e("A") join e("B"))
        assertFlowsTo(e("B"), e("A") join e("B"))
    }

    @Test
    fun meet() {
        assertFlowsTo(e("A") meet e("B"), e("A"))
        assertFlowsTo(e("A") meet e("B"), e("B"))
    }

    @Test
    fun or() {
        assertActsFor(e("A"), e("A") or e("B"))
        assertActsFor(e("B"), e("A") or e("B"))
    }

    @Test
    fun and() {
        assertActsFor(e("A") and e("B"), e("A"))
        assertActsFor(e("A") and e("B"), e("B"))
    }

    @Test
    fun `swap same`() {
        assertEquals(e("A").integrityComponent, e("A").swap().confidentialityComponent)
        assertEquals(e("A").confidentialityComponent, e("A").swap().integrityComponent)
    }

    @Nested
    inner class Bounds {
        private val bounds = SecurityLattice.Bounds(ComponentBounds)

        @Test
        fun strongest() {
            assertActsFor(bounds.strongest, e("A"))
        }

        @Test
        fun weakest() {
            assertActsFor(e("A"), bounds.weakest)
        }

        @Test
        fun bottom() {
            assertFlowsTo(bounds.bottom, e("A"))
        }

        @Test
        fun top() {
            assertFlowsTo(e("A"), bounds.top)
        }
    }
}
