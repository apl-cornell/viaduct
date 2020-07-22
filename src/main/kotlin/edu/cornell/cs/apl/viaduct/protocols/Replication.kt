package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol

/**
 * The protocol that replicates data and computations across a set of hosts in the clear.
 *
 * Replication increases integrity, but doing it in the clear sacrifices confidentiality.
 * Additionally, availability is lost if _any_ participating host aborts.
 */
class Replication(hosts: Set<Host>) : Protocol, SymmetricProtocol(hosts) {
    init {
        require(hosts.size >= 2)
    }

    companion object {
        val protocolName = "Replication"
    }

    override val protocolName: String
        get() = Replication.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.map { hostTrustConfiguration(it) }.reduce(Label::meet)

    override fun equals(other: Any?): Boolean =
        other is Replication && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()
}
