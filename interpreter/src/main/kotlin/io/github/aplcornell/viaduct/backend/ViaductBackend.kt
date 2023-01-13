package io.github.aplcornell.viaduct.backend

import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.backend.io.Strategy
import io.github.aplcornell.viaduct.backends.DefaultCombinedBackend
import io.github.aplcornell.viaduct.errors.ViaductInterpreterError
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.util.duration
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ViaductBackend")

class ViaductBackend(
    private val backends: List<ProtocolBackend>,
    private val customConnectionInfo: Map<Host, HostAddress> = mapOf(),
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
        val protocolAnalysis = ProtocolAnalysis(program, DefaultCombinedBackend.protocolComposer)

        var portNum = DEFAULT_PORT
        val connectionMap: Map<Host, HostAddress> =
            // custom connection info must provide info for all hosts or none
            if (customConnectionInfo.isEmpty()) {
                program.hostDeclarations.associate { hostDecl ->
                    val addr = HostAddress(DEFAULT_ADDRESS, portNum)
                    portNum++
                    Pair(hostDecl.name.value, addr)
                }
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

        logger.duration("runtime") { runtime.start() }
    }
}
