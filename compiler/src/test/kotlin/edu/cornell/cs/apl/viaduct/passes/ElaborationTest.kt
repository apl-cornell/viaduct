package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.PositiveTestProgramProvider
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.surface.assertStructurallyEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ElaborationTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `it is idempotent`(program: ProgramNode) {
        val elaboratedOnce = program.elaborated().toSurfaceNode()
        val elaboratedTwice = elaboratedOnce.elaborated().toSurfaceNode()
        assertStructurallyEquals(elaboratedOnce, elaboratedTwice)
    }
}
