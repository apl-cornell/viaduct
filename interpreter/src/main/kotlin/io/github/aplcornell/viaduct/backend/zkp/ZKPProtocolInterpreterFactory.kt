package io.github.aplcornell.viaduct.backend.zkp

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.backend.HostAddress
import io.github.aplcornell.viaduct.backend.ProtocolBackend
import io.github.aplcornell.viaduct.backend.ProtocolInterpreter
import io.github.aplcornell.viaduct.backend.ViaductProcessRuntime
import io.github.aplcornell.viaduct.backend.ViaductRuntime
import io.github.aplcornell.viaduct.backends.zkp.ZKP
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolProjection
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

object ZKPProtocolInterpreterFactory : ProtocolBackend {
    override fun buildProtocolInterpreters(
        host: Host,
        program: ProgramNode,
        protocols: Set<Protocol>,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductRuntime,
        connectionMap: Map<Host, HostAddress>
    ): Iterable<ProtocolInterpreter> {
        val zkpProtocols = protocols.filterIsInstance<ZKP>()
        return zkpProtocols.map {
            val processRuntime = ViaductProcessRuntime(runtime, ProtocolProjection(it, host))
            if (host == it.prover) {
                (ZKPProverInterpreter(program, protocolAnalysis, processRuntime))
            } else {
                (ZKPVerifierInterpreter(program, protocolAnalysis, processRuntime))
            }
        }
    }
}
