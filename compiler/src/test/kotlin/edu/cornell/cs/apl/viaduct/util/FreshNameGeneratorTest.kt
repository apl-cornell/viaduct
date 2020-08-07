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
        val x = freshNameGenerator.getFreshName("x_0")
        val y = freshNameGenerator.getFreshName("y_1")
        assertEquals("x_0", x)
        assertEquals("y_1", y)
    }

    @Test
    fun `generated names do not collide`() {
        val freshNameGenerator = FreshNameGenerator()
        val name1 = freshNameGenerator.getFreshName("tmp")
        val name2 = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name1, name2)
    }

    @Test
    fun `generated names with suffixes do not collide`() {
        val freshNameGenerator = FreshNameGenerator()
        freshNameGenerator.getFreshName("tmp")
        val name2: String = freshNameGenerator.getFreshName("tmp_1")
        val name3: String = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name2, name3)
    }

    @Test
    fun `generated names with suffixes do not collide 2`() {
        val freshNameGenerator = FreshNameGenerator()
        freshNameGenerator.getFreshName("tmp")
        val name2: String = freshNameGenerator.getFreshName("tmp_01")
        val name3: String = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name2, name3)
    }

    @Test
    fun `generated names with nested suffixes do not collide`() {
        val freshNameGenerator = FreshNameGenerator()
        freshNameGenerator.getFreshName("tmp")
        val name2: String = freshNameGenerator.getFreshName("tmp_1_0")
        val name3: String = freshNameGenerator.getFreshName("tmp")
        assertNotEquals(name2, name3)
    }
}
