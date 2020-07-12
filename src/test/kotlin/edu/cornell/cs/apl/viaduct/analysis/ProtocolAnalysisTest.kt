package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.protocols.SimpleSelector
import edu.cornell.cs.apl.viaduct.protocols.simpleProtocolSort
import edu.cornell.cs.apl.viaduct.selection.GreedySelection
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it does not explode`(program: ProgramNode) {
        val nameAnalysis = NameAnalysis(Tree(program.elaborated()))
        val informationFlowAnalysis = InformationFlowAnalysis(nameAnalysis)

        val dumpProtocolAssignment =
            GreedySelection(SimpleSelector(nameAnalysis, informationFlowAnalysis), ::simpleProtocolSort)
                .select(nameAnalysis.tree.root.main, nameAnalysis, informationFlowAnalysis)
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, dumpProtocolAssignment)

        nameAnalysis.tree.root.declarations.forEach {
            if (it is ProcessDeclarationNode && it.protocol.value == MainProtocol) {
                protocolAnalysis.protocols(it.body)
            }
        }
    }
}
