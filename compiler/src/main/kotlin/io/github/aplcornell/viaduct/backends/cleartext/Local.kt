package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.label
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.InputPort
import io.github.aplcornell.viaduct.syntax.OutputPort
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.values.HostValue
import io.github.aplcornell.viaduct.syntax.values.Value

/**
 * The protocol that executes code on a specific host in the clear.
 *
 * This protocol has exactly the authority and the capabilities of the host it is tied to.
 */
class Local(val host: Host) : Cleartext() {
    companion object {
        val protocolName = ProtocolName("Local")
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("host" to HostValue(host))

    override fun authority(): Label = host.label

    val inputPort = InputPort(this, this.host, INPUT)

    val hashCommitmentInputPort =
        InputPort(this, this.host, HASH_COMMITMENT_INPUT)

    val cleartextCommitmentInputPort =
        InputPort(this, this.host, CLEARTEXT_COMMITMENT_INPUT)

    val outputPort = OutputPort(this, this.host, OUTPUT)
}
