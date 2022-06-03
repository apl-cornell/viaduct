package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.PositiveTestProgramProvider
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import io.github.apl_cornell.viaduct.syntax.surface.assertStructurallyEquals
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
