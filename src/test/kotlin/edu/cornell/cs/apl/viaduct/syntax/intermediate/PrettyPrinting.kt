package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class PrettyPrintingTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `intermediate nodes can be printed`(program: ProgramNode) {
        val elaborated = program.elaborated()
        elaborated.toSurfaceNode().asDocument.print()
    }
}

