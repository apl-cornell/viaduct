package edu.cornell.cs.apl.viaduct.backends

import edu.cornell.cs.apl.viaduct.backends.aby.ABYBackend
import edu.cornell.cs.apl.viaduct.backends.aby.ABYProtocolFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.CleartextBackend
import edu.cornell.cs.apl.viaduct.backends.commitment.CommitmentBackend
import edu.cornell.cs.apl.viaduct.backends.zkp.ZKPBackend
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.unions
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

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