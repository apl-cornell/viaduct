package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.PositiveTestProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class NameAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it name checks`(program: ProgramNode) {
        NameAnalysis.get(program.elaborated()).check()
    }

    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `temporary definitions are mapped to reads`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        val nameAnalysis = NameAnalysis.get(program)
        program.descendantsIsInstance<LetNode>().forEach { declaration -> nameAnalysis.readers(declaration) }
    }
}
