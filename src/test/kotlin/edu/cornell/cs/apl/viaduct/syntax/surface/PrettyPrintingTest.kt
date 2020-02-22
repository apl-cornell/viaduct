package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.parsing.parse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class PrettyPrintingTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it is dual to parsing`(program: ProgramNode) {
        val printedAst = program.asDocument.print()
        assertStructurallyEquals(program, printedAst.parse())
    }
}
