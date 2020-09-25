package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.LabelMeet
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The protocol that replicates data and computations across a set of hosts in the clear.
 *
 * Replication increases integrity, but doing it in the clear sacrifices confidentiality.
 * Additionally, availability is lost if _any_ participating host aborts.
 */
class Replication(hosts: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Replication")
    }

    init {
        require(hosts.size >= 2)
    }

    private val participants: HostSetValue = HostSetValue(hosts)

    override val protocolName: ProtocolName
        get() = Replication.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("hosts" to participants)

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts
            .map { hostTrustConfiguration(it) }
            .reduce { acc, l -> LabelMeet(acc, l) }
            .interpret()

    val hostInputPorts: Map<Host, InputPort> =
        hosts
            .map { h -> Pair(h, InputPort(this, h, "${h.name}_INPUT")) }
            .toMap()

    val hostOutputPorts: Map<Host, OutputPort> =
        hosts
            .map { h -> Pair(h, OutputPort(this, h, "${h.name}_OUTPUT")) }
            .toMap()
}
