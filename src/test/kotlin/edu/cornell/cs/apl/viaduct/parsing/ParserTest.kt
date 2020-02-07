package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.ExampleFileProvider
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class ParserTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleFileProvider::class)
    fun `it parses`(file: File) {
        SourceFile.from(file).parse()
    }
}
