package io.github.aplcornell.viaduct.protocols

import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.values.HostSetValue
import io.github.aplcornell.viaduct.syntax.values.Value

/**
 * Protocol used to synchronize hosts.
 * Used exclusively by the backend, not meant to be used for selection!
 */
class Synchronization(hosts: Set<Host>) : Protocol() {
    companion object {
        val protocolName = ProtocolName("Synchronization")
    }

    private val participants: HostSetValue = HostSetValue(hosts)

    override val protocolName: ProtocolName
        get() = Synchronization.protocolName

    override val arguments: Map<String, Value>
        get() = mapOf("hosts" to participants)

    override fun authority(): Label = throw Error("Synchronization protocol has no authority label")
}
