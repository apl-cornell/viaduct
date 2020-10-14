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
 * The protocol that executes code on a specific host in the clear.
 *
 * This protocol has exactly the authority and the capabilities of the host it is tied to.
 */
class Local(val host: Host) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Local")
    }

    override val protocolName: ProtocolName
        get() = Local.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("host" to HostValue(host))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(host).interpret()

    val hostInputPort = InputPort(this, this.host, "INPUT")

    val hostOutputPort = OutputPort(this, this.host, "INPUT")
}
