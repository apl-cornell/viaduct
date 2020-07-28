package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.StringValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * A perfectly trusted protocol.
 *
 * The ideal protocol is not a real protocol; it is meant to be realized by real protocols.
 * Since it has infinite authority and it involves no hosts, there is no way to execute an ideal
 * protocol directly.
 *
 * @param identifier Names and distinguishes different instances.
 */
class Ideal(private val identifier: String) : Protocol() {
    override val protocolName: ProtocolName
        get() = Ideal.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("name" to StringValue(identifier))

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        Label.strongest

    override val asDocument: Document
        get() = Document(identifier)

    companion object {
        val protocolName = ProtocolName("Ideal")
    }
}
