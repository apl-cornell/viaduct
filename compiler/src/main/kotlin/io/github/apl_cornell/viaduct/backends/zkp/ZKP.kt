package io.github.apl_cornell.viaduct.backends.zkp

import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.LabelAnd
import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.security.LabelIntegrity
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.HostTrustConfiguration
import io.github.apl_cornell.viaduct.syntax.InputPort
import io.github.apl_cornell.viaduct.syntax.OutputPort
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.values.HostSetValue
import io.github.apl_cornell.viaduct.syntax.values.HostValue
import io.github.apl_cornell.viaduct.syntax.values.Value

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
