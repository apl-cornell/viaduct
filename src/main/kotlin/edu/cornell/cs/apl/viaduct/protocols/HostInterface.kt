package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The protocol that represents the external interface to a given host.
 *
 * This protocol is only used during debugging to handle [ExternalCommunicationNode]s. In normal operation,
 * communication with an instance of this protocol is replaced with communication with the actual participating host.
 */
data class HostInterface(val host: Host) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Host")
    }

    override val protocolName: ProtocolName
        get() = HostInterface.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("host" to HostValue(host))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(host)

    override val asDocument: Document
        get() = protocolName + listOf(host).tupled()
}
