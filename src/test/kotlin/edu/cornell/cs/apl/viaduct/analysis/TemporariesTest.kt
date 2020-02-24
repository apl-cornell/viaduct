package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class TemporariesTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun reads(program: ProgramNode) {
        program.elaborated().forEach {
            if (it is ProcessDeclarationNode) {
                it.body.reads()
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun readers(program: ProgramNode) {
        program.elaborated().forEach {
            if (it is ProcessDeclarationNode) {
                Readers(it)
            }
        }
    }
}
