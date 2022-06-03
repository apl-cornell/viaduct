package io.github.apl_cornell.apl.attributes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CircularAttributesTest {
    /** A very roundabout way of always returning 10... */
    private val Int.return10: Int by circularAttribute(0) {
        val next = (this + 1) % 10
        val nextValue = next.return10
        if (nextValue == 10) nextValue else nextValue + 1
    }

    @Test
    fun `circular attributes can by circular`() {
        assertEquals(10, 0.return10)
        assertEquals(10, 5.return10)
        assertEquals(10, 10.return10)
        assertEquals(10, 12.return10)
    }
}
