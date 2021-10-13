package edu.cornell.cs.apl.viaduct.backends.commitment

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
        const val INPUT = "INPUT"
        const val CLEARTEXT_INPUT = "CLEARTEXT_INPUT"
        const val OPEN_CLEARTEXT_OUTPUT = "OPEN_CLEARTEXT_OUTPUT"
        const val OPEN_COMMITMENT_OUTPUT = "OPEN_COMMITMENT_OUTPUT"
    }

    init {
        require(hashHosts.isNotEmpty())
        require(!hashHosts.contains(cleartextHost))
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("sender" to HostValue(cleartextHost), "receivers" to HostSetValue(hashHosts))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        LabelAnd(
            hostTrustConfiguration(cleartextHost),
            hashHosts
                .map { LabelIntegrity(hostTrustConfiguration(it)) }
                .reduce<LabelExpression, LabelExpression> { acc, l -> LabelAnd(acc, l) }
        ).interpret()

    val inputPort = InputPort(this, this.cleartextHost, INPUT)

    val cleartextInputPorts: Map<Host, InputPort> =
        this.hosts.map { host ->
            host to InputPort(this, host, CLEARTEXT_INPUT)
        }.toMap()

    val openCleartextOutputPort: OutputPort =
        OutputPort(this, this.cleartextHost, OPEN_CLEARTEXT_OUTPUT)

    val openCommitmentOutputPorts: Map<Host, OutputPort> =
        this.hashHosts.map { hashHost ->
            hashHost to OutputPort(this, hashHost, OPEN_COMMITMENT_OUTPUT)
        }.toMap()
}
