package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolFactory

/**
 * The protocol that executes code on a specific host in the clear.
 *
 * This protocol has exactly the authority and the capabilities of the host it is tied to.
 */
data class Local(val host: Host) : Protocol, SymmetricProtocol(setOf(host)) {
    companion object {
        const val protocolName = "Local"
    }

    override val protocolName: String
        get() = Local.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(host)

    override fun compareTo(other: Protocol): Int {
        return if (other is Local) {
            host.compareTo(other.host)
        } else {
            protocolName.compareTo(other.protocolName)
        }
    }
}

class LocalFactory : ProtocolFactory {
    override val protocolName: String
        get() = Local.protocolName

    override fun buildProtocol(participants: List<Host>): Protocol {
        assert(participants.size == 1)

        return Local(participants[0])
    }
}
