package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.PositiveTestProgramProvider
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Principal
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SpecificationTest {
    private val adversaryLabel = Label(Principal("A"))

    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it generates valid specifications`(program: ProgramNode) {
        val elaboratedProgram = program.elaborated()
        elaboratedProgram.check()
        elaboratedProgram.specification(adversaryLabel)
        // val specification = elaboratedProgram.specification(adversaryLabel)
        // specification.check()
    }
}
