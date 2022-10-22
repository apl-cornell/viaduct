package io.github.aplcornell.viaduct.syntax.surface

import io.github.aplcornell.viaduct.PositiveTestProgramProvider
import io.github.aplcornell.viaduct.backends.DefaultCombinedBackend
import io.github.aplcornell.viaduct.parsing.parse
import io.github.aplcornell.viaduct.syntax.assertStructurallyEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class PrettyPrintingTest {
    @Test
    fun `unicode characters work`() {
        val programText = "host alice"
        val program = programText.parse()
        val printedAst = program.toDocument().print()
        assertStructurallyEquals(program, printedAst.parse())
    }

    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it is dual to parsing`(program: ProgramNode) {
        val printedAst = program.toDocument().print()
        assertStructurallyEquals(program, printedAst.parse(protocolParsers = DefaultCombinedBackend.protocolParsers))
    }
}
