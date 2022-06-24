package io.github.apl_cornell.viaduct.syntax.surface

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.parse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class PrettyPrintingTest {
    @Test
    fun `unicode characters work`() {
        val programText = "host alice : {A ⊓ B}"
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
