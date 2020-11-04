package edu.cornell.cs.apl.viaduct.backend.commitment

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ProtocolProjection
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backend.ViaductRuntime
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object CommitmentProtocolInterpreterFactory : ProtocolBackend {
    override fun buildProtocolInterpreters(
        host: Host,
        program: ProgramNode,
        protocols: Set<Protocol>,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductRuntime,
        connectionMap: Map<Host, HostAddress>
    ): Iterable<ProtocolInterpreter> {
        return protocols
            .filterIsInstance<Commitment>()
            .map { protocol ->
                val processRuntime = ViaductProcessRuntime(runtime, ProtocolProjection(protocol, host))
                if (host == protocol.cleartextHost) {
                    CommitmentProtocolCleartextInterpreter(program, protocolAnalysis, processRuntime)
                } else {
                    CommitmentProtocolHashReplicaInterpreter(program, protocolAnalysis, processRuntime)
                }
            }
    }
}
