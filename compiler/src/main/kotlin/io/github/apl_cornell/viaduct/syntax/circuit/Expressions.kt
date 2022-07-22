package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.joined
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.prettyprinting.tupled
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.Operator
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.VariableNode
import io.github.apl_cornell.viaduct.syntax.surface.keyword
import io.github.apl_cornell.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node()
sealed class PureExpressionNode : ExpressionNode()
sealed class IndexExpressionNode : PureExpressionNode()

/** A literal constant. */
class LiteralNode(
    val value: Value,
    override val sourceLocation: SourceLocation
) : IndexExpressionNode() {
    override fun toDocument(): Document = value.toDocument()
}

class IndexingVariableNode(
    val name: VariableNode,
    override val sourceLocation: SourceLocation
) : IndexExpressionNode() {
    override fun toDocument(): Document = name.toDocument()
}

// Replaces Read, Query nodes.
class LookupNode(
    private val variable: VariableNode,
    private val indices: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation
) : PureExpressionNode() {
    override fun toDocument(): Document = variable + indices.bracketed()
}

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments<PureExpressionNode>,
    override val sourceLocation: SourceLocation
) : PureExpressionNode() {
    override fun toDocument(): Document = Document("(") + operator.toDocument(arguments) + ")"
}

/**
 * @param defaultValue to be used when the list is empty
 * @param operator must be associative
 */
class ReduceNode(
    val operator: Operator,
    val defaultValue: PureExpressionNode,
    val indices: Arguments<IndexParameterNode>,
    val body: PureExpressionNode,
    override val sourceLocation: SourceLocation
) : PureExpressionNode() {
    override fun toDocument(): Document {
        val args = listOf(Document("::$operator"), defaultValue)
        return keyword("reduce") + args.tupled() * "{" * indices.joined() * "->" * body * " }"
    }
}

/* Non-circuit expressions
/**
 * An external input.
 *
 * @param type Type of the value to receive.
 */
class InputNode(
    val type: ArrayTypeNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExpressionNode(), CommunicationNode {
    override fun toDocument(): Document = keyword("input") * type * keyword("from") * host
}

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExpressionNode(), CommunicationNode {
    override fun toDocument(): Document = keyword("output") * message * keyword("to") * host
}
*/
/*
  Note that index expressions may not be atomic. Consider:
      val arr[i, j] = i + (i * j)
  Can be rewritten to
      val tmp[i, j] = i * j
      val arr[i, j] = i + tmp[i, j]
  But
      val arr[i, j] = tmp[i + j + i, j]
*/
