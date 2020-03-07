package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
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
            nameAnalysis.tree.root.main().selectProtocols(nameAnalysis, informationFlowAnalysis)
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, dumpProtocolAssignment)

        nameAnalysis.tree.root.splitMain(protocolAnalysis, typeAnalysis)
    }
}
