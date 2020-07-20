package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol

data class ProtocolProjection(
    val protocol: Protocol,
    val host: Host
) {
    override fun toString(): String {
        return "${protocol.name}@$host"
    }
}
