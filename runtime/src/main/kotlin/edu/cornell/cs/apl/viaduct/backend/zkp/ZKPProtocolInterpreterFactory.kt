package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.HostAddress
import edu.cornell.cs.apl.viaduct.backend.ProtocolBackend
import edu.cornell.cs.apl.viaduct.backend.ProtocolInterpreter
import edu.cornell.cs.apl.viaduct.backend.ViaductProcessRuntime
import edu.cornell.cs.apl.viaduct.backend.ViaductRuntime
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

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
