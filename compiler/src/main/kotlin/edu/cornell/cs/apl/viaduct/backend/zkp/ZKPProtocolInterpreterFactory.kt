package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object ZKPProtocolInterpreterFactory : ProtocolInterpreterFactory {
    override fun buildProtocolInterpreter(
        program: ProgramNode,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductProcessRuntime,
        connectionMap: Map<Host, HostAddress>
    ): ProtocolInterpreter {
        return when (val protocol = runtime.projection.protocol) {
            is ZKP -> {
                if (runtime.projection.host == protocol.prover) {
                    ZKPProverInterpreter(program, protocolAnalysis, runtime)
                } else {
                    ZKPVerifierInterpreter(program, protocolAnalysis, runtime)
                }
            }

            else ->
                throw ViaductInterpreterError("Commitment: unexpected protocol $protocol")
        }
    }
}
