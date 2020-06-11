package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration

/**
 * An MPC protocol that provides security against a dishonest majority.
 * More specifically, the protocol should preserve confidentiality and integrity when up to
 * n - 1 out of the n participating hosts are corrupted.
 * In return, availability may be lost even with a single corrupted participant.
 */
class MPCWithAbort(hosts: Set<Host>) : MPCProtocol, SymmetricProtocol(hosts) {
    init {
        require(hosts.size >= 2)
    }

    companion object {
        val protocolName = "MPCWithAbort"
    }

    override val protocolName: String
        get() = MPCWithAbort.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.map { hostTrustConfiguration(it) }.reduce(Label::and)

    override fun equals(other: Any?): Boolean =
        other is MPCWithAbort && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()
}
