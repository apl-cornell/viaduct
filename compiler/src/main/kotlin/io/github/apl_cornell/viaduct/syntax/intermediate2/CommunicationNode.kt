package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.HostNode

/** An external input or an output. */
sealed interface CommunicationNode : HasSourceLocation {
    val host: HostNode
}
