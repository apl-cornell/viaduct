package io.github.aplcornell.viaduct.backend.commitment

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.backend.HostAddress
import io.github.aplcornell.viaduct.backend.ProtocolBackend
import io.github.aplcornell.viaduct.backend.ProtocolInterpreter
import io.github.aplcornell.viaduct.backend.ViaductProcessRuntime
import io.github.aplcornell.viaduct.backend.ViaductRuntime
import io.github.aplcornell.viaduct.backends.commitment.Commitment
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolProjection
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

object CommitmentProtocolInterpreterFactory : ProtocolBackend {
    override fun buildProtocolInterpreters(
        host: Host,
        program: ProgramNode,
        protocols: Set<Protocol>,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductRuntime,
        connectionMap: Map<Host, HostAddress>,
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
