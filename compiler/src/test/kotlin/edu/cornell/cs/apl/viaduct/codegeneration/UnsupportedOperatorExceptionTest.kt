package edu.cornell.cs.apl.viaduct.codegeneration

import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.Host
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
