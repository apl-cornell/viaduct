package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol

class CommitmentProtocol(val sender: Host, val recievers: Set<Host>) : Protocol {
    init {
        require(recievers.size >= 2)
        require(!recievers.contains(sender))
    }

    companion object {
        val protocolName = "Commitment"
    }

    override val hosts: Set<Host>
        get() = recievers.union(setOf(sender))

    override val name: String
        get() = protocolName

    override val protocolName: String
        get() = CommitmentProtocol.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(sender) and (recievers.map { hostTrustConfiguration(it).integrity() }.reduce(Label::and))

    override fun equals(other: Any?): Boolean =
        other is CommitmentProtocol && this.sender == other.sender && this.recievers == other.recievers

    override fun hashCode(): Int =
        hosts.hashCode()

    override val asDocument: Document
        get() = Document(protocolName)
}
