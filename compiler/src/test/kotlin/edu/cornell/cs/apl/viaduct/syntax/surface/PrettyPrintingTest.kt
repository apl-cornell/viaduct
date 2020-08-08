package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.parsing.parse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class PrettyPrintingTest {
    @Test
    fun `unicode characters work`() {
        val programText = "host alice : {A ⊓ B}"
        val program = programText.parse()
        val printedAst = program.asDocument.print()
        assertStructurallyEquals(program, printedAst.parse())
    }

    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it is dual to parsing`(program: ProgramNode) {
        val printedAst = program.asDocument.print()
        assertStructurallyEquals(program, printedAst.parse())
    }
}
