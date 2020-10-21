package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreterFactory
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object CommitmentProtocolInterpreterFactory : ProtocolInterpreterFactory {
    override fun buildProtocolInterpreter(
        program: ProgramNode,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductProcessRuntime,
        connectionMap: Map<Host, HostAddress>
    ): ProtocolInterpreter {
        return when (val protocol = runtime.projection.protocol) {
            is Commitment -> {
                if (runtime.projection.host == protocol.cleartextHost) {
                    CommitmentProtocolCleartextInterpreter(program, protocolAnalysis, runtime)
                } else {
                    CommitmentProtocolHashReplicaInterpreter(program, protocolAnalysis, runtime)
                }
            }

            else ->
                throw ViaductInterpreterError("Commitment: unexpected protocol $protocol")
        }
    }
}
