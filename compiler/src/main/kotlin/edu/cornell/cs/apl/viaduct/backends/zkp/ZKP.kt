package edu.cornell.cs.apl.viaduct.backends.zkp

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

class ZKP(val prover: Host, val verifiers: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("ZKP")
    }

    init {
        require(verifiers.isNotEmpty())
        require(!verifiers.contains(prover))
    }

    override val protocolName: ProtocolName
        get() = Companion.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("prover" to HostValue(prover), "verifiers" to HostSetValue(verifiers))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        LabelAnd(
            hostTrustConfiguration(prover),
            verifiers
                .map { LabelIntegrity(hostTrustConfiguration(it)) }
                .reduce<LabelExpression, LabelExpression> { acc, l -> LabelAnd(acc, l) }
        ).interpret()

    val secretInputPort: InputPort =
        InputPort(this, prover, "ZKP_SECRET_INPUT")

    val cleartextInput: Map<Host, InputPort> =
        (verifiers + setOf(prover)).associateWith {
            InputPort(this, it, "ZKP_PUBLIC_INPUT")
        }

    val outputPorts: Map<Host, OutputPort> =
        (verifiers + setOf(prover)).associateWith {
            OutputPort(this, it, "ZKP_OUTPUT")
        }
}
