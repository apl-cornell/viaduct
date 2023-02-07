package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.syntax.HasSourceLocation
import io.github.aplcornell.viaduct.syntax.HostNode

/** An external input or an output. */
sealed interface CommunicationNode : HasSourceLocation, PrettyPrintable {
    val host: HostNode
}
