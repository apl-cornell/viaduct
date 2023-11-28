package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.PrecircuitTestFileProvider
import io.github.aplcornell.viaduct.backends.DefaultCombinedBackend
import io.github.aplcornell.viaduct.parsing.SourceFile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class ParsingPrintingTest {
    @ParameterizedTest
    @ArgumentsSource(PrecircuitTestFileProvider::class)
    fun `parses`(file: File) {
        SourceFile.from(file).parse(DefaultCombinedBackend.protocolParsers)
    }

    @ParameterizedTest
    @ArgumentsSource(PrecircuitTestFileProvider::class)
    fun `pretty prints`(file: File) {
        val program = SourceFile.from(file).parse(DefaultCombinedBackend.protocolParsers)
        println(program.toDocument().print())
    }
}
