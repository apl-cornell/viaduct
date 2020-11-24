package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import java.io.File
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ParsingTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `it parses`(file: File) {
        SourceFile.from(file).parse()
    }
}
