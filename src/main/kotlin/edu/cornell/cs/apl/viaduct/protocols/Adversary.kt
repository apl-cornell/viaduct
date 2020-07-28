package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.passes.specification
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * The protocol that represents the adversary interface.
 *
 * Similar to [Ideal] and [HostInterface], this is not a protocol that can be realized in the real world.
 * It is used only for specifying security in the style of the universal composability framework.
 *
 * @see specification
 */
object Adversary : Protocol() {
    override val protocolName: ProtocolName
        get() = ProtocolName("Adversary")

    override val arguments: Map<String, Value>
        get() = mapOf()

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        throw UnsupportedOperationException()
}
