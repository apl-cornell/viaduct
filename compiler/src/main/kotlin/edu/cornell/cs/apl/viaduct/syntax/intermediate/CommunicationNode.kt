package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.HostNode

/** An external input or an output. */
sealed interface CommunicationNode : HasSourceLocation, PrettyPrintable {
    val host: HostNode
}
