package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class ViaductBackend(
    private val backends: Map<ProtocolName, ProtocolInterpreterFactory>
) {
    companion object {
        const val DEFAULT_PORT = 5000
        const val DEFAULT_ADDRESS = "127.0.0.1"
    }

    fun run(program: ProgramNode, host: Host) {
        val isHostValid: Boolean =
            program.declarations
                .filterIsInstance<HostDeclarationNode>()
                .map { hostDecl -> hostDecl.name.value }
                .contains(host)

        if (!isHostValid) throw ViaductInterpreterError("unknown host $host")

        // build protocol analysis from protocol annotations in the program
        val protocolAnalysis = ProtocolAnalysis(program, SimpleProtocolComposer)

        // TODO: make this configurable
        var portNum = DEFAULT_PORT
        val connectionMap: Map<Host, HostAddress> =
            program.declarations
                .filterIsInstance<HostDeclarationNode>()
                .map { hostDecl ->
                    val addr = HostAddress(DEFAULT_ADDRESS, portNum)
                    portNum++
                    Pair(hostDecl.name.value, addr)
                }
                .toMap()

        val processes: MutableMap<Process, ProcessInterpreter> = mutableMapOf()
        for (protocol in protocolAnalysis.participatingProtocols(program)) {
            if (protocol.hosts.contains(host)) {
                backends[protocol.protocolName]?.let { factory: ProtocolInterpreterFactory ->
                    val projection = ProtocolProjection(protocol, host)
                    processes[projection] = { runtime ->
                        factory.buildProtocolInterpreter(
                            program,
                            protocolAnalysis,
                            ViaductProcessRuntime(runtime, projection),
                            connectionMap
                        )
                    }
                } ?: throw Exception("no backend for protocol ${protocol.protocolName}")
            }
        }

        val runtime = ViaductRuntime(program, protocolAnalysis, connectionMap, processes, host)
        runtime.start()
    }
}
