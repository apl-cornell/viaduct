package io.github.aplcornell.viaduct.codegeneration

import io.github.aplcornell.viaduct.backends.cleartext.Local
import io.github.aplcornell.viaduct.parsing.parse
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.syntax.Host
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class UnsupportedOperatorExceptionTest {
    @Test
    fun `message is generated correctly`() {
        val program = "fun main() {}".parse().elaborated()
        assertThrows<UnsupportedOperatorException> {
            throw UnsupportedOperatorException(Local(Host("alice")), program)
        }
    }
}
