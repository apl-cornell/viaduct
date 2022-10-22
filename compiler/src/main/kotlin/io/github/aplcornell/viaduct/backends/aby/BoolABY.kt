package io.github.apl_cornell.viaduct.backends.aby

import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.InputPort
import io.github.apl_cornell.viaduct.syntax.OutputPort
import io.github.apl_cornell.viaduct.syntax.ProtocolName

class BoolABY(server: Host, client: Host) : ABY(server, client) {
    companion object {
        val protocolName = ProtocolName("BoolABY")

        const val Y2B_INPUT = "Y2B_INPUT"
        const val A2B_INPUT = "A2B_INPUT"
        const val B2Y_OUTPUT = "B2Y_OUTPUT"
        const val B2A_OUTPUT = "B2A_OUTPUT"
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    val Y2BInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, Y2B_INPUT) }

    val A2BInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, A2B_INPUT) }

    val B2YOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, B2Y_OUTPUT) }

    val B2AOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, B2A_OUTPUT) }
}
