package io.github.apl_cornell.viaduct.backend.zkp

import io.github.apl_cornell.viaduct.analysis.ProtocolAnalysis
import io.github.apl_cornell.viaduct.backend.HostAddress
import io.github.apl_cornell.viaduct.backend.ProtocolBackend
import io.github.apl_cornell.viaduct.backend.ProtocolInterpreter
import io.github.apl_cornell.viaduct.backend.ViaductProcessRuntime
import io.github.apl_cornell.viaduct.backend.ViaductRuntime
import io.github.apl_cornell.viaduct.backends.zkp.ZKP
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolProjection
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

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
            if (host == it.prover)
                (ZKPProverInterpreter(program, protocolAnalysis, processRuntime))
            else
                (ZKPVerifierInterpreter(program, protocolAnalysis, processRuntime))
        }
    }
}
