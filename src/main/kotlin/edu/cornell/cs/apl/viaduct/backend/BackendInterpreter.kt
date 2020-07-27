package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode

class BackendInterpreter(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis
) {

    companion object {
        const val DEFAULT_PORT = 5000
        const val DEFAULT_ADDRESS = "127.0.0.1"
    }

    private val backendMap: MutableMap<String, ProtocolBackend> = mutableMapOf()

    fun registerBackend(backend: ProtocolBackend) {
        for (supportedProtocol: String in backend.supportedProtocols) {
            backendMap[supportedProtocol] = backend
        }
    }

    fun run(splitProgram: ProgramNode, host: Host) {
        val isHostValid: Boolean =
            splitProgram.declarations
                .filterIsInstance<HostDeclarationNode>()
                .map { hostDecl -> hostDecl.name.value }
                .contains(host)

        if (!isHostValid) throw Exception("unknown host $host")

        // TODO: make this configurable
        var portNum = DEFAULT_PORT
        val connectionMap: Map<Host, HostAddress> =
            splitProgram.declarations
                .filterIsInstance<HostDeclarationNode>()
                .map { hostDecl ->
                    val addr = HostAddress(DEFAULT_ADDRESS, portNum)
                    portNum++
                    Pair(hostDecl.name.value, addr)
                }
                .toMap()

        val runtime = ViaductRuntime(splitProgram, connectionMap, host)

        for (decl: TopLevelDeclarationNode in splitProgram) {
            if (decl is ProcessDeclarationNode) {
                val protocol: Protocol = decl.protocol.value
                if (protocol !is HostInterface) {
                    if (protocol.hosts.contains(host)) {
                        backendMap[protocol.protocolName]?.let { backend: ProtocolBackend ->
                            val projection = ProtocolProjection(protocol, host)

                            backend.initialize(connectionMap, projection)

                            runtime.registerProcess(projection) {
                                backend.run(
                                    nameAnalysis, typeAnalysis,
                                    runtime,
                                    splitProgram,
                                    decl.body,
                                    ProtocolProjection(protocol, host)
                                )
                            }
                        } ?: throw Exception("no backend for protocol ${protocol.protocolName}")
                    }
                }
            }
        }

        runtime.start()
    }
}
