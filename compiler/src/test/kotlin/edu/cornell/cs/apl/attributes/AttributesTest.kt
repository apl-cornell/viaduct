package edu.cornell.cs.apl.attributes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AttributesTest {
    private val String.simpleLength: Int by attribute { this.length }
    private val String.recursiveLength: Int by attribute {
        if (this.isEmpty()) 0 else 1 + this.drop(1).recursiveLength
    }
    private val String.redundantLength: Int by attribute { this.simpleLength }

    private val Int.even: Boolean by attribute {
        if (this == 0) true else (this - 1).odd
    }
    private val Int.odd: Boolean by attribute {
        if (this == 1) true else (this - 1).even
    }

    private val Any.cyclic: Nothing by attribute { this.cyclic }
    private val Any.cyclicA: Nothing by attribute { this.cyclicB }
    private val Any.cyclicB: Nothing by attribute { this.cyclicA }

    @Test
    fun `simple attributes work`() {
        assertEquals("hello".length, "hello".simpleLength)
    }

    @Test
    fun `attributes can be recursive`() {
        assertEquals("hello".length, "hello".recursiveLength)
    }

    @Test
    fun `attributes can refer to other attributes`() {
        assertEquals("hello".length, "hello".redundantLength)
    }

    @Test
    fun `attributes can be mutually recursive`() {
        assertEquals(true, 0.even)
        assertEquals(true, 2.even)
        assertEquals(true, 12.even)
        assertEquals(true, 1.odd)
        assertEquals(true, 3.odd)
        assertEquals(true, 13.odd)
    }

    @Test
    fun `cyclic attributes are dynamically caught`() {
        assertThrows<CycleInAttributeDefinitionException> { 42.cyclic }
    }

    @Test
    fun `mutually cyclic attributes are dynamically caught`() {
        assertThrows<CycleInAttributeDefinitionException> { 42.cyclicA }
        assertThrows<CycleInAttributeDefinitionException> { 42.cyclicB }
    }
}
