package io.github.apl_cornell.viaduct.analysis

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.passes.check
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.StringWriter

internal class InformationFlowAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it information flow checks`(program: ProgramNode) {
        InformationFlowAnalysis.get(program.elaborated()).check()
    }

    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it has a valid constraint graph representation`(program: ProgramNode) {
        val elaboratedProgram = program.elaborated()
        val informationFlowAnalysis = InformationFlowAnalysis.get(elaboratedProgram)
        elaboratedProgram.check()
        val writer = StringWriter()
        informationFlowAnalysis.exportConstraintGraph(writer)
    }
}
