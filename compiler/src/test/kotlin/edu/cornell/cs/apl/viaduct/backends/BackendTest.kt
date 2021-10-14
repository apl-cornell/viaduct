package edu.cornell.cs.apl.viaduct.backends

import edu.cornell.cs.apl.viaduct.backends.cleartext.CleartextBackend
import edu.cornell.cs.apl.viaduct.backends.commitment.CommitmentBackend
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class BackendTest {
    @Test
    fun `unions detects duplicates`() {
        assertThrows<IllegalArgumentException> { listOf(CleartextBackend, CleartextBackend).unions() }
    }

    @Test
    fun `unions does not falsely detect duplicates`() {
        assertDoesNotThrow { listOf(CleartextBackend).unions() }
        assertDoesNotThrow { listOf(CleartextBackend, CommitmentBackend).unions() }
    }

    @Test
    fun `empty unions work`() {
        assertDoesNotThrow { listOf<Backend>().unions() }
    }
}
