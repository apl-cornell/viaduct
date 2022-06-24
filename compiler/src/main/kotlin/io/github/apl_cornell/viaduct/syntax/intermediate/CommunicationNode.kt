package io.github.apl_cornell.viaduct.syntax.intermediate

import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.HostNode

/** An external input or an output. */
sealed interface CommunicationNode : HasSourceLocation, PrettyPrintable {
    val host: HostNode
}
