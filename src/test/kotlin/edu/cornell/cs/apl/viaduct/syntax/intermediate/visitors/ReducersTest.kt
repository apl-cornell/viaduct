package edu.cornell.cs.apl.viaduct.syntax.intermediate.visitors

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ReducersTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `program size is positive`(program: ProgramNode) {
        assertTrue(program.elaborated().traverse(ProgramSize) > 0)
    }
}
