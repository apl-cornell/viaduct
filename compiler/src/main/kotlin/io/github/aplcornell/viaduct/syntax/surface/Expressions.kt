package io.github.aplcornell.viaduct.syntax.surface

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.braced
import io.github.aplcornell.viaduct.prettyprinting.nested
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.LabelNode
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.ObjectVariableNode
import io.github.aplcornell.viaduct.syntax.Operator
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.QueryNameNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.TemporaryNode
import io.github.aplcornell.viaduct.syntax.ValueTypeNode
import io.github.aplcornell.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node() {
    /** Expressions cannot have associated comments. */
    final override val comment: String?
        get() = null
}

/** An expression that requires no computation to reduce to a value. */
sealed class AtomicExpressionNode : ExpressionNode()

/** A literal constant. */
class LiteralNode(val value: Value, override val sourceLocation: SourceLocation) :
    AtomicExpressionNode() {
    override fun toDocumentWithoutComment(): Document = value.toDocument()
}

/** Reading the value stored in a temporary. */
class ReadNode(val temporary: TemporaryNode) :
    AtomicExpressionNode() {
    override val sourceLocation: SourceLocation
        get() = temporary.sourceLocation

    override fun toDocumentWithoutComment(): Document = temporary.toDocument()
}

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation,
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document = Document("(") + operator.toDocument(arguments) + ")"
}

/** A query method applied to an object. */
class QueryNode(
    val variable: ObjectVariableNode,
    val query: QueryNameNode,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation,
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document =
        IndexingNode.from(this)?.toDocument()
            ?: (variable + "." + query + arguments.tupled().nested())
}

/** Reducing the confidentiality or increasing the integrity of the result of an expression. */
sealed class DowngradeNode : ExpressionNode() {
    /** Expression whose label is being downgraded. */
    abstract val expression: ExpressionNode

    /** The label [expression] must have before the downgrade. */
    abstract val fromLabel: LabelNode?

    /** The label after the downgrade. */
    abstract val toLabel: LabelNode?
}

/** Revealing the result of an expression (reducing confidentiality). */
class DeclassificationNode(
    override val expression: ExpressionNode,
    override val fromLabel: LabelNode?,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation,
) : DowngradeNode() {
    override fun toDocumentWithoutComment(): Document {
        val from = fromLabel?.let { Document() * keyword("from") * listOf(it).braced() } ?: Document()
        val to = keyword("to") * listOf(toLabel).braced()
        return keyword("declassify") * expression + from * to
    }
}

/** Trusting the result of an expression (increasing integrity). */
class EndorsementNode(
    override val expression: ExpressionNode,
    override val fromLabel: LabelNode,
    override val toLabel: LabelNode?,
    override val sourceLocation: SourceLocation,
) : DowngradeNode() {
    override fun toDocumentWithoutComment(): Document {
        val from = keyword("from") * listOf(fromLabel).braced()
        val to = toLabel?.let { Document() * keyword("to") * listOf(it).braced() } ?: Document()
        return keyword("endorse") * expression + to * from
    }
}

// Communication Expressions

/**
 * An external input.
 *
 * @param type Type of the value to receive.
 */
class InputNode(
    val type: ValueTypeNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation,
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document = keyword("input") * type * keyword("from") * host
}

/**
 * Call to an object constructor. Used for out parameter initialization.
 */
class ConstructorCallNode(
    val objectType: ObjectTypeNode,
    val protocol: ProtocolNode?,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation,
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document {
        val arguments = arguments.tupled().nested()
        return objectType + arguments
    }
}
