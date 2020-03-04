package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it computes the correct protocols`(program: ProgramNode) {
        val protocol = Local(Host("alice"))
        val dumpProtocolAssignment = { _: Variable -> protocol }

        val nameAnalysis = NameAnalysis(Tree(program.elaborated()))
        val protocolAnalysis = ProtocolAnalysis(nameAnalysis, dumpProtocolAssignment)

        nameAnalysis.tree.root.declarations.forEach {
            if (it is ProcessDeclarationNode && it.protocol.value == MainProtocol) {
                // Most statements will be assigned to [protocol].
                // Outputs will be assigned to the protocol they are communicating with.
                assertTrue(protocolAnalysis.protocols(it.body).contains(protocol))
            }
        }
    }
}
