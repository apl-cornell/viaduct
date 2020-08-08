package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class NameAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it name checks`(program: ProgramNode) {
        NameAnalysis.get(program.elaborated()).check()
    }

    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `temporary definitions are mapped to reads`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        val nameAnalysis = NameAnalysis.get(program)
        program.letNodes().forEach { declaration -> nameAnalysis.readers(declaration) }
    }
}
