package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The [ABY](https://github.com/encryptogroup/ABY) protocol which is a two party MPC protocol
 * secure in the honest-but-curios setting.
 */
sealed class ABY(val server: Host, val client: Host) : Protocol() {
    companion object {
        const val SECRET_INPUT = "SECRET_INPUT"
        const val CLEARTEXT_INPUT = "CLEARTEXT_INPUT"
        const val CLEARTEXT_OUTPUT = "CLEARTEXT_OUTPUT"
    }

    init {
        require(server != client)
    }

    override val arguments: Map<String, Value>
        get() = mapOf("server" to HostValue(server), "client" to HostValue(client))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label {
        val combined = hostTrustConfiguration(server).interpret() join hostTrustConfiguration(client).interpret()
        // We limit confidentiality by integrity since ABY provides semi-honest security
        return combined meet combined.integrity().swap()
    }

    val secretInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, SECRET_INPUT) }

    val cleartextInputPorts: Map<Host, InputPort> =
        hosts.associateWith { h -> InputPort(this, h, CLEARTEXT_INPUT) }

    val cleartextOutputPorts: Map<Host, OutputPort> =
        hosts.associateWith { h -> OutputPort(this, h, CLEARTEXT_OUTPUT) }
}
