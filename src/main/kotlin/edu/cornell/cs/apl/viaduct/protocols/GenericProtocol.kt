package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol

// wrapper protocol used for parsing.
class GenericProtocol(
    val actualProtocolName: String,
    val participants: List<Host>
) : Protocol {

    companion object {
        const val protocolName: String = "Generic"
    }

    override val protocolName: String
        get() = GenericProtocol.protocolName

    override val name: String
        get() = protocolName

    override val hosts: Set<Host>
        get() = participants.toSet()

    override fun equals(other: Any?): Boolean =
        other is GenericProtocol &&
            this.actualProtocolName == other.actualProtocolName &&
            this.participants == other.participants

    override fun hashCode(): Int =
        participants.hashCode()

    override val asDocument: Document
        get() = Document(protocolName)

    // this shouldn't be used!
    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        Label.weakest

    // this shouldn't be used!
    override fun compareTo(other: Protocol): Int {
        return if (other is GenericProtocol) {
            actualProtocolName.compareTo(other.actualProtocolName)
        } else {
            protocolName.compareTo(other.protocolName)
        }
    }
}
