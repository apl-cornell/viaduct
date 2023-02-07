package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.CircuitTestFileProvider
import io.github.aplcornell.viaduct.backends.CircuitCodeGenerationBackend
import io.github.aplcornell.viaduct.circuitcodegeneration.compileToKotlin
import io.github.aplcornell.viaduct.parsing.SourceFile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class CircuitTest {
    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `Circuit parses`(file: File) {
        SourceFile.from(file).parse(CircuitCodeGenerationBackend.protocolParsers)
    }

    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `Circuit pretty prints`(file: File) {
        val program = SourceFile.from(file).parse(CircuitCodeGenerationBackend.protocolParsers)
        println(program.toDocument().print())
    }

    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `Circuit generates`(file: File) {
        print(
            SourceFile.from(file).parse(CircuitCodeGenerationBackend.protocolParsers).compileToKotlin(
                file.nameWithoutExtension,
                packageName = ".",
                CircuitCodeGenerationBackend::circuitCodeGenerator,
            ),
        )
    }
}
