package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.label
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.InputPort
import io.github.aplcornell.viaduct.syntax.OutputPort
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.syntax.values.Value

/**
 * The protocol that replicates data and computations across a set of hosts in the clear.
 *
 * Replication increases integrity, but doing it in the clear sacrifices confidentiality.
 * Additionally, availability is lost if _any_ participating host aborts.
 */
class Replication(hosts: Set<Host>) : Cleartext() {
    companion object {
        val protocolName = ProtocolName("Replication")
    }

    init {
        require(hosts.size >= 2)
    }

    private val participants: HostSetValue = HostSetValue(hosts)

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("hosts" to participants)

    override fun authority(): Label =
        hosts.map { it.label }.reduce(Label::meet)

    val hostInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, INPUT) }

    val hostHashCommitmentInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, HASH_COMMITMENT_INPUT) }

    val hostCleartextCommitmentInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, CLEARTEXT_COMMITMENT_INPUT) }

    val hostOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, OUTPUT) }
}
