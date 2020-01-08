package edu.cornell.cs.apl.viaduct.imp.ast2

import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue

/** A pure computation. */
sealed class ExpressionNode : Node()

/** An expression that requires no computation to reduce to a value. */
sealed class AtomicExpressionNode : ExpressionNode()

/** A literal constant. */
data class LiteralNode(val value: ImpValue) : AtomicExpressionNode()

/** Reading the value stored in a temporary. */
data class ReadNode(val temporary: Temporary) : AtomicExpressionNode()

/** An n-ary operator applied to n arguments. */
data class OperatorApplicationNode(val operator: Operator, val arguments: List<ExpressionNode>) :
    ExpressionNode()

/** A query method call on an assignable. */
data class QueryNode(
    val assignable: Assignable,
    val query: Query,
    val arguments: List<ExpressionNode>
) : ExpressionNode()
