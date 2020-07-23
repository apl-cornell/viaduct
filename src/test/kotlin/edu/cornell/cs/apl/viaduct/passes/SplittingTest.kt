package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.selection.SimpleSelection
import edu.cornell.cs.apl.viaduct.selection.SimpleSelector
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class SplittingTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it splits`(program: ProgramNode) {
        val nameAnalysis = NameAnalysis(Tree(program.elaborated()))
        val typeAnalysis = TypeAnalysis(nameAnalysis)
        val informationFlowAnalysis = InformationFlowAnalysis(nameAnalysis)

        val dumpProtocolAssignment =
            SimpleSelection(
                SimpleSelector(
                    nameAnalysis,
                    informationFlowAnalysis
                ), ::simpleProtocolCost
            )
                .select(nameAnalysis.tree.root.main, nameAnalysis, informationFlowAnalysis)
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, dumpProtocolAssignment)

        val splitProgram = nameAnalysis.tree.root.splitMain(protocolAnalysis, typeAnalysis)
        edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode(
            // TODO: don't remove [HostInterface]s once [splitMain] starts renaming communication with main.
            declarations = splitProgram.filterNot { it is ProcessDeclarationNode && it.protocol.value is HostInterface },
            sourceLocation = splitProgram.sourceLocation
        ).check()
    }
}
