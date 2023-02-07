package io.github.aplcornell.viaduct.backends.commitment

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

class Commitment(val cleartextHost: Host, val hashHosts: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Commitment")
        const val INPUT = "INPUT"
        const val CLEARTEXT_INPUT = "CLEARTEXT_INPUT"
        const val OPEN_CLEARTEXT_OUTPUT = "OPEN_CLEARTEXT_OUTPUT"
        const val OPEN_COMMITMENT_OUTPUT = "OPEN_COMMITMENT_OUTPUT"
    }

    init {
        require(hashHosts.isNotEmpty())
        require(!hashHosts.contains(cleartextHost))
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("sender" to HostValue(cleartextHost), "receivers" to HostSetValue(hashHosts))

    override fun authority(): Label =
        cleartextHost.label and
            hashHosts
                .map { it.label.integrity() }
                .reduce { acc, l -> acc and l }

    val inputPort = InputPort(this, this.cleartextHost, INPUT)

    val cleartextInputPorts: Map<Host, InputPort> =
        this.hosts.associateWith { host ->
            InputPort(this, host, CLEARTEXT_INPUT)
        }

    val openCleartextOutputPort: OutputPort =
        OutputPort(this, this.cleartextHost, OPEN_CLEARTEXT_OUTPUT)

    val openCommitmentOutputPorts: Map<Host, OutputPort> =
        this.hashHosts.associateWith { hashHost ->
            OutputPort(this, hashHost, OPEN_COMMITMENT_OUTPUT)
        }
}
