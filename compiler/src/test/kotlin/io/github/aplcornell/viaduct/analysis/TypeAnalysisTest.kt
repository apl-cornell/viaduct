package io.github.aplcornell.viaduct.analysis

import io.github.aplcornell.viaduct.PositiveTestProgramProvider
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class TypeAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it type checks`(program: ProgramNode) {
        TypeAnalysis.get(program.elaborated()).check()
    }
}
