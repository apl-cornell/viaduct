package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import edu.cornell.cs.apl.viaduct.backends.CodeGenerationBackend
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.passes.compileToKotlin
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class CodeGeneratorTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `it generates`(file: File) {
        if (file.parentFile.name != "code-generation") return

        SourceFile.from(file).compileToKotlin(
            file.nameWithoutExtension,
            packageName = ".",
            backend = CodeGenerationBackend
        ).writeTo(System.out)
    }
}
