package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.selection.SimpleSelection
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SplittingTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it splits`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()

        val dumbProtocolAssignment =
            SimpleSelection(program, simpleProtocolFactory(program), ::simpleProtocolCost).select(program)
        val protocolAnalysis = ProtocolAnalysis(program, dumbProtocolAssignment)

        val nameAnalysis = NameAnalysis.get(program)
        val typeAnalysis = TypeAnalysis.get(program)
        val splitProgram =
            Splitter(nameAnalysis, protocolAnalysis, typeAnalysis)
                .splitMain(program)

        edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode(
            // TODO: don't remove [HostInterface]s once [splitMain] starts renaming communication with main.
            declarations = splitProgram.filterNot { it is ProcessDeclarationNode && it.protocol.value is HostInterface },
            sourceLocation = splitProgram.sourceLocation
        ).check()
    }
}
