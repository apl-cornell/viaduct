package io.github.aplcornell.viaduct.codegeneration

import io.github.aplcornell.viaduct.PositiveTestFileProvider
import io.github.aplcornell.viaduct.backends.CodeGenerationBackend
import io.github.aplcornell.viaduct.parsing.SourceFile
import io.github.aplcornell.viaduct.passes.compileToKotlin
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class CodeGeneratorTest {
    @ParameterizedTest
    @ArgumentsSource(CodeGenerationFileProvider::class)
    fun `it generates`(file: File) {
        SourceFile.from(file).compileToKotlin(
            file.nameWithoutExtension,
            packageName = ".",
            backend = CodeGenerationBackend,
        ).writeTo(System.out)
    }
}

internal class CodeGenerationFileProvider : ArgumentsProvider by PositiveTestFileProvider("code-generation")
