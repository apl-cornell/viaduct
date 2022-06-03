package io.github.apl_cornell.viaduct.backend.commitment

import io.github.apl_cornell.viaduct.analysis.ProtocolAnalysis
import io.github.apl_cornell.viaduct.backend.HostAddress
import io.github.apl_cornell.viaduct.backend.ProtocolBackend
import io.github.apl_cornell.viaduct.backend.ProtocolInterpreter
import io.github.apl_cornell.viaduct.backend.ViaductProcessRuntime
import io.github.apl_cornell.viaduct.backend.ViaductRuntime
import io.github.apl_cornell.viaduct.backends.commitment.Commitment
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolProjection
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

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
