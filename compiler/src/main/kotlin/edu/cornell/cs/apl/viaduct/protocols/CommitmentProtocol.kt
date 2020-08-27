package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.LabelAnd
import edu.cornell.cs.apl.viaduct.security.LabelExpression
import edu.cornell.cs.apl.viaduct.security.LabelIntegrity
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

class CommitmentProtocol(val sender: Host, receivers: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Commitment")
    }

    init {
        require(receivers.size >= 2)
        require(!receivers.contains(sender))
    }

    val receivers = HostSetValue(receivers)

    override val protocolName: ProtocolName
        get() = CommitmentProtocol.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("sender" to HostValue(sender), "receivers" to receivers)

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        LabelAnd(
            hostTrustConfiguration(sender),
            receivers
                .map { LabelIntegrity(hostTrustConfiguration(it)) }
                .reduce<LabelExpression, LabelExpression> { acc, l -> LabelAnd(acc, l) }
        ).interpret()
}
