package io.github.aplcornell.viaduct.backends.aby

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

    @Suppress("ktlint:standard:property-naming")
    val Y2AInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, Y2A_INPUT) }

    @Suppress("ktlint:standard:property-naming")
    val B2AInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, B2A_INPUT) }

    @Suppress("ktlint:standard:property-naming")
    val A2YOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, A2Y_OUTPUT) }

    @Suppress("ktlint:standard:property-naming")
    val A2BOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, A2B_OUTPUT) }
}
