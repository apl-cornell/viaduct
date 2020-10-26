package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.InputPort
import edu.cornell.cs.apl.viaduct.syntax.OutputPort
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The [ABY](https://github.com/encryptogroup/ABY) protocol which is a two party MPC protocol
 * secure in the honest-but-curios setting.
 */
open class ABY(val server: Host, val client: Host) : Protocol() {
    companion object {
        val protocolName = ProtocolName("ABY")
        const val SECRET_INPUT = "SECRET_INPUT"
        const val CLEARTEXT_INPUT = "CLEARTEXT_INPUT"
        const val CLEARTEXT_OUTPUT = "CLEARTEXT_OUTPUT"
    }

    init {
        require(server != client)
    }

    override val protocolName: ProtocolName
        get() = ABY.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("server" to HostValue(server), "client" to HostValue(client))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(server).interpret() and hostTrustConfiguration(client).interpret()

    val secretInputPorts: Map<Host, InputPort> =
        hosts
            .map { h -> Pair(h, InputPort(this, h, SECRET_INPUT)) }
            .toMap()

    val cleartextInputPorts: Map<Host, InputPort> =
        hosts
            .map { h -> Pair(h, InputPort(this, h, CLEARTEXT_INPUT)) }
            .toMap()

    val cleartextOutputPorts: Map<Host, OutputPort> =
        hosts
            .map { h -> Pair(h, OutputPort(this, h, CLEARTEXT_OUTPUT)) }
            .toMap()
}
