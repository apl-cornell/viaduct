package io.github.aplcornell.viaduct.circuitcodegeneration

import io.github.aplcornell.viaduct.CircuitTestFileProvider
import io.github.aplcornell.viaduct.backends.CircuitCodeGenerationBackend
import io.github.aplcornell.viaduct.parsing.SourceFile
import io.github.aplcornell.viaduct.syntax.circuit.parse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class CodeGeneratorTest {
    @ParameterizedTest
    @ArgumentsSource(CircuitTestFileProvider::class)
    fun `it generates`(file: File) {
        SourceFile.from(file).parse(CircuitCodeGenerationBackend.protocolParsers).compileToKotlin(
            file.nameWithoutExtension,
            packageName = ".",
            CircuitCodeGenerationBackend::circuitCodeGenerator,
        ).writeTo(System.out)
    }
}
