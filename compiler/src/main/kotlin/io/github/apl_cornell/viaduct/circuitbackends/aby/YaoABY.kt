package io.github.apl_cornell.viaduct.circuitbackends.aby

import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.InputPort
import io.github.apl_cornell.viaduct.syntax.OutputPort
import io.github.apl_cornell.viaduct.syntax.ProtocolName

class YaoABY(server: Host, client: Host) : ABY(server, client) {
    companion object {
        val protocolName = ProtocolName("YaoABY")

        const val B2Y_INPUT = "B2Y_INPUT"
        const val A2Y_INPUT = "A2Y_INPUT"
        const val Y2B_OUTPUT = "Y2B_OUTPUT"
        const val Y2A_OUTPUT = "Y2A_OUTPUT"
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    val B2YInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, B2Y_INPUT) }

    val A2YInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, A2Y_INPUT) }

    val Y2BOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, Y2B_OUTPUT) }

    val Y2AOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, Y2A_OUTPUT) }
}
