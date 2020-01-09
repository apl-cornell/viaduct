package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Query
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.ImmutableList

/** A pure computation. */
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
    val arguments: ImmutableList<ExpressionNode>,
    override val sourceLocation: SourceLocation
) :
    ExpressionNode()

/** A query method call on an object. */
data class QueryNode(
    val variable: ObjectVariable,
    val query: Query,
    val arguments: ImmutableList<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : ExpressionNode()

/** Reducing the confidentiality or increasing the integrity of the result of an expression. */
sealed class DowngradeNode : ExpressionNode() {
    /** Expression whose label is being downgraded. */
    abstract val expression: ExpressionNode

    /** The label [expression] must have before the downgrade. */
    abstract val fromLabel: Label

    /** The label after the downgrade. */
    abstract val toLabel: Label
}

/** Revealing the the result of an expression (reducing confidentiality). */
data class DeclassificationNode(
    override val expression: ExpressionNode,
    override val fromLabel: Label,
    override val toLabel: Label,
    override val sourceLocation: SourceLocation
) : DowngradeNode()

/** Trusting the the result of an expression (increasing integrity). */
data class EndorsementNode(
    override val expression: ExpressionNode,
    override val fromLabel: Label,
    override val toLabel: Label,
    override val sourceLocation: SourceLocation
) : DowngradeNode()
