package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

class Commitment(val cleartextHost: Host, val hashHosts: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Commitment")
    }

    init {
        require(hashHosts.size >= 1)
        require(!hashHosts.contains(cleartextHost))
    }

    val receivers = HostSetValue(hashHosts)

    override val protocolName: ProtocolName
        get() = Commitment.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("sender" to HostValue(cleartextHost), "receivers" to receivers)

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(cleartextHost) and (receivers.map { hostTrustConfiguration(it).integrity() }.reduce(Label::and))
}
