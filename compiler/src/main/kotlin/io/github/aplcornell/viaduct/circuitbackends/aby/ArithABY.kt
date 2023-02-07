package io.github.aplcornell.viaduct.circuitbackends.aby

import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.InputPort
import io.github.aplcornell.viaduct.syntax.OutputPort
import io.github.aplcornell.viaduct.syntax.ProtocolName

class ArithABY(server: Host, client: Host) : ABY(server, client) {
    companion object {
        val protocolName = ProtocolName("ArithABY")

        const val Y2A_INPUT = "Y2A_INPUT"
        const val B2A_INPUT = "B2A_INPUT"
        const val A2Y_OUTPUT = "A2Y_OUTPUT"
        const val A2B_OUTPUT = "A2B_OUTPUT"
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    val Y2AInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, Y2A_INPUT) }

    val B2AInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, B2A_INPUT) }

    val A2YOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, A2Y_OUTPUT) }

    val A2BOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, A2B_OUTPUT) }
}
