package io.github.apl_cornell.viaduct.parsing

import io.github.apl_cornell.viaduct.analysis.mainFunction
import io.github.apl_cornell.viaduct.prettyprinting.DefaultStyle
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.surface.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class SourceRangeTest {
    @Nested
    inner class ShowInSource {
        private val program1 = "fun main() {}".parse()
        private val program2 = "fun main() {\n}".parse()
        private val program3 = """
                host h1 : {H1}
                host h2 : {H2}
                fun main() {}
                host h3 : {H3}
                host h4 : {H4}
        """.trimIndent().parse()

        @Test
        fun `it underlines single-line ranges`() {
            assertEquals(2, program1.showInSource().lines().size)
        }

        @Test
        fun `it marks multi-line ranges`() {
            assertEquals(3, program2.showInSource().lines().size)
        }

        @Test
        fun `it adds context`() {
            assertEquals(2, program3.main.showInSource().lines().size)
            assertEquals(5, program3.main.showInSource(contextLines = 1).lines().size)
            assertEquals(7, program3.main.showInSource(contextLines = 2).lines().size)
        }

        @Test
        fun `it does not add more context than there exists`() {
            assertEquals(7, program3.main.showInSource(contextLines = 100).lines().size)
        }

        @Test
        fun `it adds a single blank line at the end`() {
            val nodes: List<HasSourceLocation> = listOf(program1.main, program2.main, program3.main)
            for (node in nodes) {
                for (contextLines in 0..2) {
                    val lines = node.showInSource(contextLines).lines()
                    Assertions.assertTrue(isBlankOrUnderline(lines.last())) {
                        "Last line should be blank."
                    }
                    Assertions.assertFalse(isBlankOrUnderline(lines[lines.size - 2])) {
                        "There should be no more than one blank line."
                    }
                }
            }
        }
    }
}

/**
 * Returns true if [line] contains only space and carrot (^) characters.
 * Carrots are considered blank since they are used for underlining portions of the previous line.
 */
internal fun isBlankOrUnderline(line: String): Boolean =
    line.all { it == ' ' || it == '^' }

/** Display an AST node in source and print what is displayed to ease debugging tests. */
private fun HasSourceLocation.showInSource(contextLines: Int = 0): String =
    this.sourceLocation.showInSource(DefaultStyle, contextLines).print().also {
        println(">>>>>\n$it\n<<<<<")
    }

/** Returns the declaration of the [mainFunction] function in this program. */
private val ProgramNode.main: FunctionDeclarationNode
    get() = this.find { it is FunctionDeclarationNode && it.name.value == mainFunction } as FunctionDeclarationNode
