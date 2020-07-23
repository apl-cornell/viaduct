package edu.cornell.cs.apl.viaduct.protocols
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolFactory
import edu.cornell.cs.apl.viaduct.util.asComparable

/**
 * An MPC protocol that provides security against a dishonest majority.
 * More specifically, the protocol should preserve confidentiality and integrity when up to
 * n - 1 out of the n participating hosts are corrupted.
 * In return, availability may be lost even with a single corrupted participant.
 */
class ABY(hosts: Set<Host>) : MPCProtocol, SymmetricProtocol(hosts) {
    init {
        require(hosts.size >= 2)
    }

    companion object {
        const val protocolName = "MPCWithAbort"
    }

    override val protocolName: String
        get() = ABY.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.map { hostTrustConfiguration(it) }.reduce(Label::and)

    override fun equals(other: Any?): Boolean =
        other is ABY && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()

    override fun compareTo(other: Protocol): Int {
        return if (other is ABY) {
            hosts.asComparable().compareTo(other.hosts)
        } else {
            protocolName.compareTo(other.protocolName)
        }
    }
}

class ABYFactory : ProtocolFactory {
    override val protocolName: String
        get() = ABY.protocolName

    override fun buildProtocol(participants: List<Host>): Protocol {
        return ABY(participants.toSet())
    }
}
