package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.PositiveTestProgramProvider
import edu.cornell.cs.apl.viaduct.backends.DefaultCombinedBackend
import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.passes.annotateWithProtocols
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.selection.CostMode
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it does not explode`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        program.check()

        val protocolComposer = DefaultCombinedBackend.protocolComposer
        val protocolAssignment = selectProtocolsWithZ3(
            program,
            DefaultCombinedBackend.protocolFactory(program), protocolComposer,
            SimpleCostEstimator(protocolComposer, SimpleCostRegime.LAN),
            CostMode.MINIMIZE
        )

        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)
        val protocolAnalysis = ProtocolAnalysis(annotatedProgram, protocolComposer)

        try {
            protocolAnalysis.protocols(annotatedProgram.main.body)
        } catch (_: NoMainError) {
            // Do nothing
        }
    }
}
