package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.LabelAnd
import edu.cornell.cs.apl.viaduct.security.LabelExpression
import edu.cornell.cs.apl.viaduct.security.LabelIntegrity
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
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

    override val protocolName: ProtocolName
        get() = Commitment.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("sender" to HostValue(cleartextHost), "receivers" to HostSetValue(hashHosts))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        LabelAnd(
            hostTrustConfiguration(cleartextHost),
            hashHosts
                .map { LabelIntegrity(hostTrustConfiguration(it)) }
                .reduce<LabelExpression, LabelExpression> { acc, l -> LabelAnd(acc, l) }
        ).interpret()

    val cleartextInputPorts: Map<Host, InputPort> =
        hashHosts.union(setOf(cleartextHost))
            .map { h -> Pair(h, InputPort(this, h, "COMMITMENT_CLEARTEXT_INPUT")) }
            .toMap()

    val cleartextSecretInputPort: Map<Host, InputPort> =
        mapOf(cleartextHost to InputPort(this, cleartextHost, "COMMITMENT_SECRET_INPUT"))

    val cleartextOutputPort: OutputPort =
        OutputPort(this, cleartextHost, "COMMITMENT_CLEARTEXT_OUTPUT")

}
