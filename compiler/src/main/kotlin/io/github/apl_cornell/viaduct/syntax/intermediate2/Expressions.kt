package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.Operator
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.ValueTypeNode
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node()
sealed class PureExpressionNode : ExpressionNode()
sealed class IndexExpressionNode : PureExpressionNode()

/** A literal constant. */
class LiteralNode(
    val value: Value,
    override val sourceLocation: SourceLocation
) : IndexExpressionNode()

class IndexingVariableNode(
    val name: VariableNode,
    override val sourceLocation: SourceLocation
) : IndexExpressionNode()

// Replaces Query nodes.
class LookupNode(
    private val variable: VariableNode,
    private val indices: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation
) : PureExpressionNode()

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments<PureExpressionNode>,
    override val sourceLocation: SourceLocation
) : PureExpressionNode()

/**
 * @param defaultValue to be used when the list is empty
 * @param operator must be associative
 */
class ReduceNode(
    val defaultValue: PureExpressionNode,
    val operator: Operator,
    val indices: Arguments<IndexParameterNode>,
    val body: PureExpressionNode,
    override val sourceLocation: SourceLocation
) : PureExpressionNode()

/**
 * An external input.
 *
 * @param type Type of the value to receive.
 */
class InputNode(
    val type: ValueTypeNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExpressionNode(), CommunicationNode

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExpressionNode(), CommunicationNode

/*
  Note that index expressions may not be atomic. Consider:
      val arr[i, j] = i + (i * j)
  Can be rewritten to
      val tmp[i, j] = i * j
      val arr[i, j] = i + tmp[i, j]
  But
      val arr[i, j] = tmp[i + j + i, j]
*/
