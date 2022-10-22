package io.github.apl_cornell.viaduct.codegeneration

import io.github.apl_cornell.viaduct.backends.cleartext.Local
import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.syntax.Host
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
