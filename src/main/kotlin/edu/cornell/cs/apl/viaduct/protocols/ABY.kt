package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * An MPC protocol that provides security against a dishonest majority.
 * More specifically, the protocol should preserve confidentiality and integrity when up to
 * n - 1 out of the n participating hosts are corrupted.
 * In return, availability may be lost even with a single corrupted participant.
 */
class ABY(hosts: Set<Host>) : Protocol() {
    init {
        require(hosts.size >= 2)
    }

    private val participants: HostSetValue = HostSetValue(hosts)

    override val protocolName: ProtocolName
        get() = ABY.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("hosts" to participants)

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.map { hostTrustConfiguration(it) }.reduce(Label::and)

    companion object {
        val protocolName = ProtocolName("ABY")
    }
}
