package io.github.aplcornell.viaduct.syntax

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.security.LabelExpression
import io.github.aplcornell.viaduct.syntax.circuit.ArrayType
import io.github.aplcornell.viaduct.syntax.datatypes.ClassName
import io.github.aplcornell.viaduct.syntax.datatypes.MethodName
import io.github.aplcornell.viaduct.syntax.datatypes.QueryName
import io.github.aplcornell.viaduct.syntax.datatypes.UpdateName
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.values.Value

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

typealias LabelVariableNode = Located<LabelVariable>

typealias JumpLabelNode = Located<JumpLabel>

typealias ValueNode = Located<Value>

typealias ValueTypeNode = Located<ValueType>

typealias ArrayTypeNode = Located<ArrayType>

typealias LabelNode = Located<LabelExpression>

typealias FunctionNameNode = Located<FunctionName>
