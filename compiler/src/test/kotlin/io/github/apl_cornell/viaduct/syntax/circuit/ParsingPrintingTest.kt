package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.CircuitTestFileProvider
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.SourceFile
import io.github.apl_cornell.viaduct.syntax.assertStructurallyEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class ParsingPrintingTest {
    private val protocolParsers = DefaultCombinedBackend.protocolParsers

    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `printing is dual to parsing`(file: File) {
        val program = SourceFile.from(file).parse(protocolParsers)
        val printedAst = program.toDocument().print()
        assertStructurallyEquals(program, printedAst.parse(protocolParsers = protocolParsers))
    }
}
