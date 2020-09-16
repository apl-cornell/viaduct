package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode

class BackendInterpreter(
    private val backendMap: Map<ProtocolName, ProtocolBackend>
) {
    companion object {
        const val DEFAULT_PORT = 5000
        const val DEFAULT_ADDRESS = "127.0.0.1"
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

        val processes: MutableMap<Process, ProcessBody> = mutableMapOf()
        for (decl: TopLevelDeclarationNode in splitProgram) {
            if (decl is ProcessDeclarationNode) {
                val protocol: Protocol = decl.protocol.value
                if (protocol !is HostInterface) {
                    if (protocol.hosts.contains(host)) {
                        backendMap[protocol.protocolName]?.let { backend: ProtocolBackend ->
                            val projection = ProtocolProjection(protocol, host)

                            backend.initialize(connectionMap, projection)

                            processes[projection] = { runtime ->
                                backend.run(
                                    ViaductProcessRuntime(runtime, projection),
                                    splitProgram,
                                    decl.body
                                )
                            }
                        } ?: throw Exception("no backend for protocol ${protocol.protocolName}")
                    }
                }
            }
        }

        val runtime = ViaductRuntime(splitProgram, connectionMap, processes, host)
        runtime.start()
    }
}
