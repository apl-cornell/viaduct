package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SplittingTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it splits`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated().specialize()

        program.check()
        val protocolAssignment =
            selectProtocolsWithZ3(program, program.main, SimpleProtocolFactory(program), SimpleCostEstimator)
        val annotatedProgram = program.annotateWithProtocols(protocolAssignment)
        val protocolAnalysis = ProtocolAnalysis(annotatedProgram, SimpleProtocolComposer)
        val splitProgram = Splitter(protocolAnalysis).splitMain()

        edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode(
            // TODO: don't remove [HostInterface]s once [splitMain] starts renaming communication with main.
            declarations = splitProgram.filterNot { it is ProcessDeclarationNode && it.protocol.value is HostInterface },
            sourceLocation = splitProgram.sourceLocation
        ).check()
    }
}
