package io.github.apl_cornell.viaduct.analysis

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
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