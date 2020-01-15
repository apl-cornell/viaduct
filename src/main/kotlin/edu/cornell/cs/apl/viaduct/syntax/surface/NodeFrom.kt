package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/**
 * Attaches a source location to an arbitrary type making it an abstract syntax tree node.
 */
data class NodeFrom<T>(val value: T, override val sourceLocation: SourceLocation) : Node()

typealias HostNode = NodeFrom<Host>

typealias ProtocolNode = NodeFrom<Protocol>

typealias ValueTypeNode = NodeFrom<ValueType>

typealias LabelNode = NodeFrom<Label>

typealias ObjectVariableNode = NodeFrom<ObjectVariable>

typealias TemporaryNode = NodeFrom<Temporary>
