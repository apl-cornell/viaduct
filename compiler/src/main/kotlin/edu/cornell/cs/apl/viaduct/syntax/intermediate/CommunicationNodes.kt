package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode

/** A node for sending or receiving messages. */
interface CommunicationNode : HasSourceLocation, PrettyPrintable

/** Communication happening between a protocol and a host. */
interface ExternalCommunicationNode : CommunicationNode {
    val host: HostNode
}

/** Communication happening between protocols. */
interface InternalCommunicationNode : CommunicationNode {
    val protocol: ProtocolNode
}
