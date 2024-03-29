package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.integrity
import io.github.aplcornell.viaduct.security.label
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.InputPort
import io.github.aplcornell.viaduct.syntax.OutputPort
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.syntax.values.HostValue
import io.github.aplcornell.viaduct.syntax.values.Value

class ZKP(val prover: Host, val verifiers: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("ZKP")
    }

    init {
        require(verifiers.isNotEmpty())
        require(!verifiers.contains(prover))
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("prover" to HostValue(prover), "verifiers" to HostSetValue(verifiers))

    override fun authority(): Label =
        prover.label and
            verifiers
                .map { it.label.integrity() }
                .reduce { acc, l -> acc and l }

    val secretInputPort: InputPort =
        InputPort(this, prover, "ZKP_SECRET_INPUT")

    val cleartextInput: Map<Host, InputPort> =
        (verifiers + setOf(prover)).associateWith {
            InputPort(this, it, "ZKP_PUBLIC_INPUT")
        }

    val outputPorts: Map<Host, OutputPort> =
        (verifiers + setOf(prover)).associateWith {
            OutputPort(this, it, "ZKP_OUTPUT")
        }
}
