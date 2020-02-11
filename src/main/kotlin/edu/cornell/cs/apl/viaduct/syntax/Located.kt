package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/** Attaches a source location to an arbitrary type. */
data class Located<out T>(val value: T, override val sourceLocation: SourceLocation) :
    HasSourceLocation

typealias HostNode = Located<Host>

typealias ProtocolNode = Located<Protocol>

typealias ClassNameNode = Located<ClassName>

typealias TemporaryNode = Located<Temporary>

typealias ObjectVariableNode = Located<ObjectVariable>

typealias JumpLabelNode = Located<JumpLabel>

typealias ValueTypeNode = Located<ValueType>

typealias LabelNode = Located<Label>
