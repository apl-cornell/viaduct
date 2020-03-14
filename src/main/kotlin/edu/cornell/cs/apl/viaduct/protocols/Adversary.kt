package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.passes.specification
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import kotlinx.collections.immutable.persistentSetOf

/**
 * The protocol that represents the adversary interface.
 *
 * Similar to [Ideal] and [HostInterface], this is not a protocol that can be realized in the real world.
 * It is used only for specifying security in the style of the universal composability framework.
 *
 * @see specification
 */
object Adversary : Protocol {
    override val protocolName: String
        get() = "Adversary"

    override val hosts: Set<Host>
        get() = persistentSetOf()

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        throw UnsupportedOperationException()

    override val name: String
        get() = protocolName

    override val asDocument: Document
        get() = Document(protocolName)
}
