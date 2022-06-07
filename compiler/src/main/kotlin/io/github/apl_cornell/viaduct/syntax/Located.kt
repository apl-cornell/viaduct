package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.syntax.datatypes.ClassName
import io.github.apl_cornell.viaduct.syntax.datatypes.MethodName
import io.github.apl_cornell.viaduct.syntax.datatypes.QueryName
import io.github.apl_cornell.viaduct.syntax.datatypes.UpdateName
import io.github.apl_cornell.viaduct.syntax.types.ValueType
import io.github.apl_cornell.viaduct.syntax.values.Value

/** Attaches a source location to an arbitrary type. */
data class Located<out T : PrettyPrintable>(
    val value: T,
    override val sourceLocation: SourceLocation
) : HasSourceLocation, PrettyPrintable {
    override fun toDocument(): Document = value.toDocument()
}

typealias HostNode = Located<Host>

typealias ProtocolNode = Located<Protocol>

typealias ProtocolNameNode = Located<ProtocolName>

typealias ClassNameNode = Located<ClassName>

typealias MethodNameNode = Located<MethodName>

typealias QueryNameNode = Located<QueryName>

typealias UpdateNameNode = Located<UpdateName>

typealias ArgumentLabelNode = Located<ArgumentLabel>

typealias VariableNode = Located<Variable>

typealias TemporaryNode = Located<Temporary>

typealias ObjectVariableNode = Located<ObjectVariable>

typealias JumpLabelNode = Located<JumpLabel>

typealias ValueNode = Located<Value>

typealias ValueTypeNode = Located<ValueType>

typealias LabelNode = Located<LabelExpression>

typealias FunctionNameNode = Located<FunctionName>
