package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName

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
        hosts
            .map { h -> Pair(h, InputPort(this, h, Y2B_INPUT)) }
            .toMap()

    val A2BInputPorts: Map<Host, InputPort> =
        hosts
            .map { h -> Pair(h, InputPort(this, h, A2B_INPUT)) }
            .toMap()

    val B2YOutputPorts: Map<Host, OutputPort> =
        hosts
            .map { h -> Pair(h, OutputPort(this, h, B2Y_OUTPUT)) }
            .toMap()

    val B2AOutputPorts: Map<Host, OutputPort> =
        hosts
            .map { h -> Pair(h, OutputPort(this, h, B2A_OUTPUT)) }
            .toMap()
}
