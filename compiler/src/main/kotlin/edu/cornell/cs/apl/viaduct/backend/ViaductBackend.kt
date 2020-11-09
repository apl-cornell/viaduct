package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.backend.IO.Strategy
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class ViaductBackend(
    private val backends: List<ProtocolBackend>,
    private val customConnectionInfo: Map<Host, HostAddress> = mapOf()
) {
    companion object {
        const val DEFAULT_PORT = 5000
        const val DEFAULT_ADDRESS = "127.0.0.1"
    }

    fun run(program: ProgramNode, host: Host, strategy: Strategy) {
        val isHostValid: Boolean =
            program.declarations
                .filterIsInstance<HostDeclarationNode>()
                .map { hostDecl -> hostDecl.name.value }
                .contains(host)

        if (!isHostValid) throw ViaductInterpreterError("unknown host $host")

        // build protocol analysis from protocol annotations in the program
        val protocolAnalysis = ProtocolAnalysis(program, SimpleProtocolComposer)

        var portNum = DEFAULT_PORT
        val connectionMap: Map<Host, HostAddress> =
            // custom connection info must provide info for all hosts or none
            if (customConnectionInfo.isEmpty()) {
                program.hostDeclarations
                    .map { hostDecl ->
                        val addr = HostAddress(DEFAULT_ADDRESS, portNum)
                        portNum++
                        Pair(hostDecl.name.value, addr)
                    }.toMap()
            } else {
                val allHostsDefined = program.hostDeclarations.all { hostDecl ->
                    customConnectionInfo.containsKey(hostDecl.name.value)
                }
                if (!allHostsDefined) {
                    throw ViaductInterpreterError("Some hosts do not have connection information.")
                }
                customConnectionInfo
            }

        val runtime = ViaductRuntime(host, program, protocolAnalysis, connectionMap, backends, strategy)
        runtime.start()
    }
}
