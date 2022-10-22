package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.CircuitTestFileProvider
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.SourceFile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class ParsingPrintingTest {
    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `Circuit parses`(file: File) {
        SourceFile.from(file).parse(DefaultCombinedBackend.protocolParsers)
    }

    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `Circuit prettyprints`(file: File) {
        val program = SourceFile.from(file).parse(DefaultCombinedBackend.protocolParsers)
        println(program.toDocument().print())
    }
}
