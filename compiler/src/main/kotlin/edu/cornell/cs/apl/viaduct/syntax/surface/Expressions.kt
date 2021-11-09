package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.bracketed
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.security.LabelExpression
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.values.Value

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
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document = Document("(") + operator.toDocument(arguments) + ")"
}

/** A query method applied to an object. */
class QueryNode(
    val variable: ObjectVariableNode,
    val query: QueryNameNode,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
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
    override val sourceLocation: SourceLocation
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
    override val sourceLocation: SourceLocation
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
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document = keyword("input") * type * keyword("from") * host
}

/**
 * Call to an object constructor. Used for out parameter initialization.
 */
class ConstructorCallNode(
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    val labelArguments: Arguments<Located<LabelExpression>>?,
    val protocol: ProtocolNode?,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override fun toDocumentWithoutComment(): Document {
        val types = typeArguments.bracketed().nested()
        val labels =
            labelArguments
                ?.map { arg -> listOf(arg).braced() }
                ?.joined()
                ?: Document()
        val arguments = arguments.tupled().nested()
        return className + types + labels + arguments
    }
}
