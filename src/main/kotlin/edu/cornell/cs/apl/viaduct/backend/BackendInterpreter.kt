package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode

class BackendInterpreter(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis
) {
    private val backendMap: MutableMap<String, ProtocolBackend> = mutableMapOf()

    fun registerBackend(backend: ProtocolBackend) {
        for (supportedProtocol: String in backend.supportedProtocols) {
            backendMap[supportedProtocol] = backend
        }
    }

    fun run(splitProgram: ProgramNode, host: Host) {
        val runtime = ViaductRuntime(host)

        for (decl: TopLevelDeclarationNode in splitProgram) {
            if (decl is ProcessDeclarationNode) {
                val protocol: Protocol = decl.protocol.value
                if (protocol !is HostInterface) {
                    backendMap[protocol.protocolName]?.let { backend: ProtocolBackend ->
                        backend.run(
                            nameAnalysis, typeAnalysis,
                            runtime,
                            splitProgram,
                            decl.body,
                            ProtocolProjection(protocol, host)
                        )
                    }
                }
            }
        }
    }
}
