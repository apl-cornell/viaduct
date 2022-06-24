package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.PositiveTestProgramProvider
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
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
