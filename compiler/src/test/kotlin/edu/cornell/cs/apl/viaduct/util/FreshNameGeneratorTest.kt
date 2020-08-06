package edu.cornell.cs.apl.viaduct.util

import org.junit.jupiter.api.Test

class FreshNameGeneratorTest {
    @Test
    fun suffixStripTest() {
        val nameGenerator = FreshNameGenerator()
        nameGenerator.getFreshName("tmp")
        val name2: String = nameGenerator.getFreshName("tmp_1")
        val name3: String = nameGenerator.getFreshName("tmp")

        assert(name2 != name3)
    }

    @Test
    fun suffixStripTest2() {
        val nameGenerator = FreshNameGenerator()
        nameGenerator.getFreshName("tmp")
        val name2: String = nameGenerator.getFreshName("tmp_01")
        val name3: String = nameGenerator.getFreshName("tmp")

        assert(name2 != name3)
    }
}
