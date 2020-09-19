package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.ExampleProgramProvider
import edu.cornell.cs.apl.viaduct.errors.NoMainError
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolCost
import edu.cornell.cs.apl.viaduct.selection.simpleProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ProtocolAnalysisTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleProgramProvider::class)
    fun `it does not explode`(surfaceProgram: ProgramNode) {
        val program = surfaceProgram.elaborated()

        program.check()
        val dumpProtocolAssignment =
            selectProtocolsWithZ3(program, program.main, simpleProtocolFactory(program), ::simpleProtocolCost)
        val protocolAnalysis = ProtocolAnalysis(program, dumpProtocolAssignment)

        try {
            protocolAnalysis.protocols(program.main.body)
        } catch (_: NoMainError) {
            // Do nothing
        }
    }
}
