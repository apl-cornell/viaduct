package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import edu.cornell.cs.apl.viaduct.errors.NoMainError
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class ParsingTest {
    @Test
    fun `the parser does not crash on the empty input`() {
        assertThrows<NoMainError> { "".parse() }
    }

    @Test
    fun `the parser does not crash on blank input`() {
        assertThrows<NoMainError> { " ".parse() }
        assertThrows<NoMainError> { " \n ".parse() }
    }

    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `it parses`(file: File) {
        SourceFile.from(file).parse()
    }
}
