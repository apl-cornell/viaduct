package edu.cornell.cs.apl.viaduct.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class FreshNameGeneratorTest {
    @Test
    fun `already fresh names are unchanged`() {
        val freshNameGenerator = FreshNameGenerator()
        val x = freshNameGenerator.getFreshName("x")
        val y = freshNameGenerator.getFreshName("y")
        assertEquals("x", x)
        assertEquals("y", y)
    }

    @Test
    fun `already fresh names with suffixes are unchanged`() {
        val freshNameGenerator = FreshNameGenerator()
        val x0 = freshNameGenerator.getFreshName("x_0")
        val x1 = freshNameGenerator.getFreshName("x_1")
        val y1 = freshNameGenerator.getFreshName("y_1")
        val y0 = freshNameGenerator.getFreshName("y_0")
        assertEquals("x_0", x0)
        assertEquals("x_1", x1)
        assertEquals("y_0", y0)
        assertEquals("y_1", y1)
    }

    @Test
    fun `generated names do not collide`() {
        val freshNameGenerator = FreshNameGenerator()
        val name1 = freshNameGenerator.getFreshName("tmp")
        val name2 = freshNameGenerator.getFreshName("tmp")
        val name3 = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name1, name2)
        assertNotEquals(name1, name3)
        assertNotEquals(name2, name3)
    }

    @Test
    fun `generated names with suffixes do not collide`() {
        val freshNameGenerator = FreshNameGenerator()
        val name1 = freshNameGenerator.getFreshName("tmp")
        val name2 = freshNameGenerator.getFreshName("tmp_1")
        val name3 = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name1, name2)
        assertNotEquals(name1, name3)
        assertNotEquals(name2, name3)
    }

    @Test
    fun `generated names with suffixes do not collide 2`() {
        val freshNameGenerator = FreshNameGenerator()
        val name1 = freshNameGenerator.getFreshName("tmp")
        val name2 = freshNameGenerator.getFreshName("tmp")
        val name3 = freshNameGenerator.getFreshName("tmp_1")
        assertNotEquals(name1, name2)
        assertNotEquals(name1, name3)
        assertNotEquals(name2, name3)
    }

    @Test
    fun `generated names with nested suffixes do not collide`() {
        val freshNameGenerator = FreshNameGenerator()
        val name1 = freshNameGenerator.getFreshName("tmp")
        val name2 = freshNameGenerator.getFreshName("tmp_1_0")
        val name3 = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name1, name2)
        assertNotEquals(name1, name3)
        assertNotEquals(name2, name3)
    }
}
