package io.github.apl_cornell.viaduct.backends

import io.github.apl_cornell.viaduct.backends.cleartext.CleartextBackend
import io.github.apl_cornell.viaduct.backends.commitment.CommitmentBackend
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class BackendTest {
    @Test
    fun `unions detects duplicate protocols`() {
        assertThrows<IllegalArgumentException> { listOf(CleartextBackend, CleartextBackend).unions() }
    }

    @Test
    fun `unions does not falsely detect duplicate protocols`() {
        assertDoesNotThrow { listOf(CleartextBackend).unions() }
        assertDoesNotThrow { listOf(CleartextBackend, CommitmentBackend).unions() }
    }

    @Test
    fun `empty unions work`() {
        assertDoesNotThrow { listOf<Backend>().unions() }
    }

    @Test
    fun `unions detects missing protocol parsers`() {
        assertThrows<IllegalArgumentException> { listOf(MissingProtocolParser).unions() }
    }

    @Test
    fun `unions detects extraneous protocol parsers`() {
        assertThrows<IllegalArgumentException> { listOf(ExtraProtocolParser).unions() }
    }

    private object MissingProtocolParser : Backend by CleartextBackend {
        override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
            get() = mapOf()
    }

    private object ExtraProtocolParser : Backend by CleartextBackend {
        override val protocols: Set<ProtocolName>
            get() = setOf()
    }
}
