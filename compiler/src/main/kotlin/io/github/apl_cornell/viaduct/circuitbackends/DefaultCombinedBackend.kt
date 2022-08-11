package io.github.apl_cornell.viaduct.circuitbackends

// import io.github.apl_cornell.viaduct.circuitbackends.aby.ABYProtocolFactory
import io.github.apl_cornell.viaduct.circuitbackends.aby.ABYBackend
import io.github.apl_cornell.viaduct.circuitbackends.cleartext.CleartextBackend

/** A back end that combines the cleartext, commitment, ZKP, and ABY back ends. */
object DefaultCombinedBackend : Backend by circuitbackends.unions() {
//    override fun protocolFactory(program: ProgramNode): ProtocolFactory {
//        // TODO: fix ABYProtocolFactory to get rid of this hack
//        val factories = circuitbackends.map { it.protocolFactory(program) }
//        val combinedFactory = factories.unions()
//        val abyFactory = factories.last() as ABYProtocolFactory
//        abyFactory.parentFactory = combinedFactory
//        abyFactory.protocolComposer = protocolComposer
//        return combinedFactory
//    }
}

private val circuitbackends = listOf(CleartextBackend, ABYBackend)
