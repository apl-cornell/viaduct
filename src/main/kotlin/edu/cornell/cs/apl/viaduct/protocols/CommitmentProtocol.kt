package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.util.asComparable

class CommitmentProtocol(val sender: Host, val receivers: Set<Host>) : Protocol {
    init {
        require(receivers.size >= 2)
        require(!receivers.contains(sender))
    }

    companion object {
        const val protocolName = "Commitment"
    }

    override val hosts: Set<Host>
        get() = receivers.union(setOf(sender))

    override val name: String
        get() = protocolName

    override val protocolName: String
        get() = CommitmentProtocol.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(sender) and (receivers.map { hostTrustConfiguration(it).integrity() }.reduce(Label::and))

    override fun equals(other: Any?): Boolean =
        other is CommitmentProtocol && this.sender == other.sender && this.receivers == other.receivers

    override fun hashCode(): Int =
        hosts.hashCode()

    override val asDocument: Document
        get() = Document(protocolName)

    override fun compareTo(other: Protocol): Int {
        return if (other is CommitmentProtocol) {
            val senderCmp: Int = sender.compareTo(other.sender)
            if (senderCmp != 0) {
                senderCmp
            } else {
                receivers.asComparable().compareTo(other.receivers)
            }
        } else {
            protocolName.compareTo(other.protocolName)
        }
    }
}
