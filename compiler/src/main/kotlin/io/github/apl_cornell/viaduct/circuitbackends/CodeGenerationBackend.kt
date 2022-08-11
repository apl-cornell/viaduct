package io.github.apl_cornell.viaduct.circuitbackends

//import io.github.apl_cornell.viaduct.circuitbackends.aby.ABYProtocolFactory
import io.github.apl_cornell.viaduct.circuitbackends.aby.ABYBackend
import io.github.apl_cornell.viaduct.circuitbackends.cleartext.CleartextBackend

/** Combines all back ends that support code generation. */
object CodeGenerationBackend : Backend by circuitbackends.unions() {
//    override fun protocolFactory(program: ProgramNode): ProtocolFactory {
//        // Whoever is computing the length needs to pass it to ABY
//        // at the ABY cleartext port
//        val factories = circuitbackends.map { it.protocolFactory(program) }
//        val combinedFactory = factories.unions()
//        val abyFactory = factories.last() as ABYProtocolFactory
//        abyFactory.parentFactory = combinedFactory
//        abyFactory.protocolComposer = protocolComposer
//        return combinedFactory
//    }
}

private val circuitbackends = listOf(CleartextBackend, ABYBackend)
