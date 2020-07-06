
package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol

class CommitmentProtocol(val sender : Host, val recievers : Set<Host>) : Protocol {
   init {
       require(recievers.size >= 2)
       require(!recievers.contains(sender))
   }

   companion object {
       val protocolName = "Commitment"
   }

    override val hosts : Set<Host>
        get() = recievers.union(setOf(sender))

    override val name : String
        get() = protocolName

    override val protocolName: String
        get() = Replication.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        TODO("authority for commitment")

    override fun equals(other: Any?): Boolean =
        other is Replication && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()

    override val asDocument: Document
        get() = Document(protocolName)

}
