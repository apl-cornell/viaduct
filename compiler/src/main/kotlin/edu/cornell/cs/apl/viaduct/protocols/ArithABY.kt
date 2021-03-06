package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName

class ArithABY(server: Host, client: Host) : ABY(server, client) {
    companion object {
        val protocolName = ProtocolName("ArithABY")

        const val Y2A_INPUT = "Y2A_INPUT"
        const val B2A_INPUT = "B2A_INPUT"
        const val A2Y_OUTPUT = "A2Y_OUTPUT"
        const val A2B_OUTPUT = "A2B_OUTPUT"
    }

    override val protocolName: ProtocolName
        get() = ArithABY.protocolName

    val Y2AInputPorts: Map<Host, InputPort> =
        hosts
            .map { h -> Pair(h, InputPort(this, h, Y2A_INPUT)) }
            .toMap()

    val B2AInputPorts: Map<Host, InputPort> =
        hosts
            .map { h -> Pair(h, InputPort(this, h, B2A_INPUT)) }
            .toMap()

    val A2YOutputPorts: Map<Host, OutputPort> =
        hosts
            .map { h -> Pair(h, OutputPort(this, h, A2Y_OUTPUT)) }
            .toMap()

    val A2BOutputPorts: Map<Host, OutputPort> =
        hosts
            .map { h -> Pair(h, OutputPort(this, h, A2B_OUTPUT)) }
            .toMap()
}
