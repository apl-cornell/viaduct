package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExternalCommunicationNode

/**
 * The protocol that represents the external interface to a given host.
 *
 * This protocol is only used during debugging to handle [ExternalCommunicationNode]s. In normal operation,
 * communication with an instance of this protocol is replaced with communication with the actual participating host.
 */
data class HostInterface(val host: Host) : Protocol, SymmetricProtocol(setOf(host)) {
    override val protocolName: String
        get() = "Host"

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration.getValue(host)
}
