package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import kotlinx.collections.immutable.persistentSetOf

/**
 * The protocol that executes code on a specific host in the clear.
 *
 * This protocol has exactly the authority and the capabilities of the host it is tied to.
 */
data class Local(val host: Host) : Protocol {
    override val name: String
        get() = "Local"

    override val hosts: Set<Host> = persistentSetOf(host)

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration.getValue(host)
}
