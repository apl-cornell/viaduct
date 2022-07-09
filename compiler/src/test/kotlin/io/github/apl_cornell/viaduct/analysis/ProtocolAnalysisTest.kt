package io.github.apl_cornell.viaduct.analysis

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.errors.NoMainError
import io.github.apl_cornell.viaduct.passes.annotateWithProtocols
import io.github.apl_cornell.viaduct.passes.check
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.selection.ProtocolSelection
import io.github.apl_cornell.viaduct.selection.SelectionProblemSolver
import io.github.apl_cornell.viaduct.selection.SimpleCostEstimator
import io.github.apl_cornell.viaduct.selection.SimpleCostRegime
import io.github.apl_cornell.viaduct.selection.selectionProblemSolvers
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(SelectionProblemProvider::class)
    fun `it does not explode`(surfaceProgram: ProgramNode, solver: SelectionProblemSolver) {
        val program = surfaceProgram.elaborated()
        program.check()

        val protocolComposer = DefaultCombinedBackend.protocolComposer
        val protocolAssignment =
            ProtocolSelection(
                solver,
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

/** Pairs every [ProgramNode] with every [SelectionProblemSolver]. */
private class SelectionProblemProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        val programs = PositiveTestProgramProvider().provideArguments(context)
        return programs.flatMap { program ->
            val solvers = selectionProblemSolvers.map { it.second }.stream()
            solvers.map { solver ->
                Arguments.of(program.get().first(), solver)
            }
        }
    }
}
