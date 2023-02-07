package io.github.aplcornell.viaduct.backends

import io.github.aplcornell.viaduct.backends.aby.ABYBackend
import io.github.aplcornell.viaduct.backends.aby.ABYProtocolFactory
import io.github.aplcornell.viaduct.backends.cleartext.CleartextBackend
import io.github.aplcornell.viaduct.backends.commitment.CommitmentBackend
import io.github.aplcornell.viaduct.backends.zkp.ZKPBackend
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.selection.unions
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

/** A back end that combines the cleartext, commitment, ZKP, and ABY back ends. */
object DefaultCombinedBackend : Backend by backends.unions() {
    override fun protocolFactory(program: ProgramNode): ProtocolFactory {
        // TODO: fix ABYProtocolFactory to get rid of this hack
        val factories = backends.map { it.protocolFactory(program) }
        val combinedFactory = factories.unions()
        val abyFactory = factories.last() as ABYProtocolFactory
        abyFactory.parentFactory = combinedFactory
        abyFactory.protocolComposer = protocolComposer
        return combinedFactory
    }
}

private val backends = listOf(CleartextBackend, CommitmentBackend, ZKPBackend, ABYBackend)
