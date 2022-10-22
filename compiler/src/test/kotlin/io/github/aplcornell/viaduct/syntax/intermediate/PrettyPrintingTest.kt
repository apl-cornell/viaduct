package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.PositiveTestProgramProvider
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.syntax.surface.ProgramNode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class PrettyPrintingTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestProgramProvider::class)
    fun `intermediate nodes can be printed`(program: ProgramNode) {
        program.elaborated().toDocument().print()
    }
}
