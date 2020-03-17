package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Principal
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SpecificationTest {
    private val adversaryLabel = Label.create(Principal.create("A"))

    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it generates valid specifications`(program: ProgramNode) {
        val specification = program.elaborated().specification(adversaryLabel)
        specification.check()
    }
}
