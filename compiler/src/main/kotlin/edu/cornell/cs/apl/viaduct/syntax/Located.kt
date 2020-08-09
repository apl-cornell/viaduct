package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ClassName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MethodName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.QueryName
import edu.cornell.cs.apl.viaduct.syntax.datatypes.UpdateName
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** Attaches a source location to an arbitrary type. */
data class Located<out T : PrettyPrintable>(
    val value: T,
    override val sourceLocation: SourceLocation
) : HasSourceLocation, PrettyPrintable {
    override val asDocument: Document
        get() = value.asDocument
}

typealias HostNode = Located<Host>

typealias ProtocolNode = Located<Protocol>

typealias ProtocolNameNode = Located<ProtocolName>

typealias ClassNameNode = Located<ClassName>

typealias MethodNameNode = Located<MethodName>

typealias QueryNameNode = Located<QueryName>

typealias UpdateNameNode = Located<UpdateName>

typealias ArgumentLabelNode = Located<ArgumentLabel>

typealias TemporaryNode = Located<Temporary>

typealias ObjectVariableNode = Located<ObjectVariable>

typealias JumpLabelNode = Located<JumpLabel>

typealias ValueNode = Located<Value>

typealias ValueTypeNode = Located<ValueType>

typealias LabelNode = Located<Label>

typealias FunctionNameNode = Located<FunctionName>
