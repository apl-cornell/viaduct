package io.github.apl_cornell.viaduct.backends

import io.github.apl_cornell.viaduct.backends.aby.ABYBackend
import io.github.apl_cornell.viaduct.backends.aby.ABYProtocolFactory
import io.github.apl_cornell.viaduct.backends.cleartext.CleartextBackend
import io.github.apl_cornell.viaduct.backends.commitment.CommitmentBackend
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.selection.unions
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

/** Combines all back ends that support code generation. */
object CodeGenerationBackend : Backend by backends.unions() {
    override fun protocolFactory(program: ProgramNode): ProtocolFactory {
        // Whoever is computing the length needs to pass it to ABY
        // at the ABY cleartext port
        val factories = backends.map { it.protocolFactory(program) }
        val combinedFactory = factories.unions()
        val abyFactory = factories.last() as ABYProtocolFactory
        abyFactory.parentFactory = combinedFactory
        abyFactory.protocolComposer = protocolComposer
        return combinedFactory
    }
}

private val backends = listOf(CleartextBackend, CommitmentBackend, ABYBackend)
