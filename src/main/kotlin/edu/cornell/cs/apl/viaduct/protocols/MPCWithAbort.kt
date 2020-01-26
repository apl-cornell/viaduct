package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import kotlinx.collections.immutable.toPersistentSet

/**
 * An MPC protocol that provides security against a dishonest majority.
 * More specifically, the protocol should preserve confidentiality and integrity when up to
 * n - 1 out of the n participating hosts are corrupted.
 * In return, availability may be lost even with a single corrupted participant.
 */
class MPCWithAbort(hosts: Set<Host>) : MPCProtocol() {
    init {
        require(hosts.size >= 2)
    }

    override val protocolName: String
        get() = "MPCWithAbort"

    // Make an immutable copy
    override val hosts: Set<Host> = hosts.toPersistentSet()

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.fold(Label.weakest()) { label, host -> label.and(hostTrustConfiguration.getValue(host)) }
}
