package io.github.aplcornell.viaduct.parsing

import io.github.aplcornell.viaduct.PositiveTestFileProvider
import io.github.aplcornell.viaduct.backends.DefaultCombinedBackend
import io.github.aplcornell.viaduct.errors.NoMainError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

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
        SourceFile.from(file).parse(DefaultCombinedBackend.protocolParsers)
    }
}
