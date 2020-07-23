package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import kotlinx.collections.immutable.persistentSetOf

/**
 * A perfectly trusted protocol.
 *
 * The ideal protocol is not a real protocol; it is meant to be realized by real protocols.
 * Since it has infinite authority and it involves no hosts, there is no way to execute an ideal
 * protocol directly.
 *
 * @param name Names and distinguishes different instances.
 */
data class Ideal(override val name: String) : Protocol {
    override val protocolName: String
        get() = "Ideal"

    override val hosts: Set<Host>
        get() = persistentSetOf()

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        Label.strongest

    override val asDocument: Document
        get() = Document(name)

    override fun compareTo(other: Protocol): Int {
        return if (other is Ideal) {
            name.compareTo(other.name)
        } else {
            protocolName.compareTo(other.protocolName)
        }
    }
}
