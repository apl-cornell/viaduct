package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SpecializationTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it specializes`(program: ProgramNode) {
        val elaborated = program.elaborated()
        elaborated.check()
        elaborated.specialize()
    }
    /*
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it has a valid constraint graph representation`(program: ProgramNode) {
        val elaboratedProgram = program.elaborated()
        val informationFlowAnalysis = InformationFlowAnalysis.get(elaboratedProgram)
        elaboratedProgram.check()
        val writer = StringWriter()
        informationFlowAnalysis.exportConstraintGraph(writer)
    }*/
}
