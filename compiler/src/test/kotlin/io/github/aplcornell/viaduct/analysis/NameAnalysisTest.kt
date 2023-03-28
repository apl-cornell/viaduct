package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.PositiveTestProgramProvider
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class NameAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it name checks`(program: ProgramNode) {
        program.elaborated().analyses.get<NameAnalysis>().check()
    }

    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `temporary definitions are mapped to reads`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        val nameAnalysis = program.analyses.get<NameAnalysis>()
        program.descendantsIsInstance<LetNode>().forEach { declaration -> nameAnalysis.readers(declaration) }
    }
}
