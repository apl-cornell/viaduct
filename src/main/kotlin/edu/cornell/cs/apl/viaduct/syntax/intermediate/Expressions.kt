package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Query
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node()

/** An expression that requires no computation to reduce to a value. */
sealed class AtomicExpressionNode : ExpressionNode()

/** A literal constant. */
data class LiteralNode(val value: Value, override val sourceLocation: SourceLocation) :
    AtomicExpressionNode()

/** Reading the value stored in a temporary. */
data class ReadNode(val temporary: Temporary, override val sourceLocation: SourceLocation) :
    AtomicExpressionNode()

/** An n-ary operator applied to n arguments. */
data class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments,
    override val sourceLocation: SourceLocation
) : ExpressionNode()

/** A query method applied to an object. */
data class QueryNode(
    val variable: ObjectVariableNode,
    val query: Query,
    val arguments: Arguments,
    override val sourceLocation: SourceLocation
) : ExpressionNode()

/** Reducing the confidentiality or increasing the integrity of the result of an expression. */
sealed class DowngradeNode : ExpressionNode() {
    /** Expression whose label is being downgraded. */
    abstract val expression: AtomicExpressionNode

    /** The label [expression] must have before the downgrade. */
    abstract val fromLabel: LabelNode?

    /** The label after the downgrade. */
    abstract val toLabel: LabelNode
}

/** Revealing the the result of an expression (reducing confidentiality). */
data class DeclassificationNode(
    override val expression: AtomicExpressionNode,
    override val fromLabel: LabelNode? = null,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation
) : DowngradeNode()

/** Trusting the result of an expression (increasing integrity). */
data class EndorsementNode(
    override val expression: AtomicExpressionNode,
    override val fromLabel: LabelNode? = null,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation
) : DowngradeNode()
