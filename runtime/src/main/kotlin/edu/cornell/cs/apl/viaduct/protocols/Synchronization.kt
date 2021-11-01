package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

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
