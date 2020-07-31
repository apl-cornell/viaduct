package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.protocols.MainProtocol
import edu.cornell.cs.apl.viaduct.selection.SimpleSelection
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.selection.simpleSelector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it does not explode`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()

        val dumpProtocolAssignment =
            SimpleSelection(program, simpleSelector(program), ::simpleProtocolCost).select(program.main)
        val protocolAnalysis = ProtocolAnalysis(program, dumpProtocolAssignment)

        program.tree.root.declarations.forEach {
            if (it is ProcessDeclarationNode && it.protocol.value == MainProtocol) {
                protocolAnalysis.protocols(it.body)
            }
        }
    }
}
