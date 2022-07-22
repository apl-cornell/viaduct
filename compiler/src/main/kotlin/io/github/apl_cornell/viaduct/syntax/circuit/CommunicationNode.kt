package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.HostNode

/** An external input or an output. */
sealed interface CommunicationNode : HasSourceLocation {
    val host: HostNode
}
