package io.github.apl_cornell.viaduct.protocols

import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.HostTrustConfiguration
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.values.HostSetValue
import io.github.apl_cornell.viaduct.syntax.values.Value

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

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        throw Error("Synchronization protocol has no authority label")
}
