package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.security.Label

/**
 * An abstract location where computations can be placed.
 *
 * A protocol simultaneously names a location and determines the (cryptographic) mechanism for
 * executing the code placed at that location.
 */
interface Protocol : Name {
    /** Protocol name. */
    val protocolName: String

    /** Hosts involved in this protocol. */
    val hosts: Set<Host>

    /**
     * Computes the authority label of this protocol given the authority labels of the
     * participating hosts.
     */
    fun authority(hostTrustConfiguration: HostTrustConfiguration): Label

    override val nameCategory: String
        get() = "protocol"
}
