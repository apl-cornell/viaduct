package io.github.aplcornell.viaduct.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class ListsTest {
    @Nested
    inner class Subsequences {
        @Test
        fun `the empty list has one subsequence`() {
            assertEquals(listOf(listOf<Int>()), listOf<Int>().subsequences())
        }

        @Test
        fun `singleton lists have two subsequences`() {
            assertEquals(listOf(listOf(), listOf(1)), listOf(1).subsequences())
        }

        @Test
        fun `subsequences are sorted from the smallest to the largest`() {
            assertEquals(
                listOf(listOf(), listOf(1), listOf(2), listOf(1, 2)),
                listOf(1, 2).subsequences()
            )
        }

        @Test
        fun `duplicate elements are duplicated`() {
            assertEquals(
                listOf(listOf(), listOf(1), listOf(1), listOf(1, 1)),
                listOf(1, 1).subsequences()
            )
        }
    }
}
