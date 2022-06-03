package io.github.apl_cornell.viaduct.analysis

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.errors.NoMainError
import io.github.apl_cornell.viaduct.passes.annotateWithProtocols
import io.github.apl_cornell.viaduct.passes.check
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.selection.ProtocolSelection
import io.github.apl_cornell.viaduct.selection.SimpleCostEstimator
import io.github.apl_cornell.viaduct.selection.SimpleCostRegime
import io.github.apl_cornell.viaduct.selection.Z3Selection
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it does not explode`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()
        program.check()

        val protocolComposer = DefaultCombinedBackend.protocolComposer
        val protocolAssignment =
            ProtocolSelection(
                Z3Selection(),
                DefaultCombinedBackend.protocolFactory(program),
                protocolComposer,
                SimpleCostEstimator(protocolComposer, SimpleCostRegime.LAN)
            ).selectAssignment(program)

        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)
        val protocolAnalysis = ProtocolAnalysis(annotatedProgram, protocolComposer)

        try {
            protocolAnalysis.protocols(annotatedProgram.main.body)
        } catch (_: NoMainError) {
            // Do nothing
        }
    }
}
