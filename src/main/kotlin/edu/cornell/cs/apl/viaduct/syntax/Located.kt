package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/**
 * Attaches a source location to an arbitrary type making it an abstract syntax tree node.
 */
data class Located<T>(val value: T, override val sourceLocation: SourceLocation) : HasSourceLocation

typealias HostNode = Located<Host>

typealias ProtocolNode = Located<Protocol>

typealias ValueTypeNode = Located<ValueType>

typealias LabelNode = Located<Label>

typealias ObjectVariableNode = Located<ObjectVariable>

typealias TemporaryNode = Located<Temporary>
