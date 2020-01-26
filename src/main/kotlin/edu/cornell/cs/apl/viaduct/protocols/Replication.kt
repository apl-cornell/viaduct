package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import kotlinx.collections.immutable.toPersistentSet

/**
 * The protocol that replicates data and computations across a set of hosts in the clear.
 *
 * Replication increases integrity, but doing it in the clear sacrifices confidentiality.
 * Additionally, availability is lost if _any_ participating host aborts.
 */
class Replication(hosts: Set<Host>) : Protocol {
    init {
        require(hosts.size >= 2)
    }

    override val protocolName: String
        get() = "Replication"

    // Make an immutable copy
    override val hosts: Set<Host> = hosts.toPersistentSet()

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.fold(Label.top()) { label, host -> label.meet(hostTrustConfiguration.getValue(host)) }

    override fun equals(other: Any?): Boolean =
        other is Replication && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()
}
