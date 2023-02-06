package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.prettyprinting.tupled
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.Operator
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.surface.keyword
import io.github.apl_cornell.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node()
sealed class IndexExpressionNode : ExpressionNode()

/** A literal constant. */
class LiteralNode(
    val value: Value,
    override val sourceLocation: SourceLocation
) : IndexExpressionNode() {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document = value.toDocument()
}

class ReferenceNode(
    val name: VariableNode,
    override val sourceLocation: SourceLocation
) : IndexExpressionNode() {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document = name.toDocument()
}

class LookupNode(
    val variable: VariableNode,
    val indices: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override val children: Iterable<Node>
        get() = indices

    override fun toDocument(): Document = variable + indices.bracketed()
}

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: OperatorNode,
    val arguments: Arguments<ExpressionNode>,
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override val children: Iterable<Node>
        get() = listOf(operator) + arguments

    override fun toDocument(): Document = Document("(") + operator.operator.toDocument(arguments) + ")"
}

class OperatorNode(
    val operator: Operator,
    override val sourceLocation: SourceLocation
) : Node() {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document = Document("::$operator")
}

/**
 * @param defaultValue to be used when the list is empty
 * @param operator must be associative
 */
class ReduceNode(
    val operator: OperatorNode,
    val defaultValue: ExpressionNode,
    val indices: IndexParameterNode,
    val body: ExpressionNode,
    override val sourceLocation: SourceLocation
) : ExpressionNode() {
    override val children: Iterable<Node>
        get() = listOf(operator, defaultValue, indices, body)

    override fun toDocument(): Document {
        return keyword("reduce") + listOf(operator, defaultValue).tupled() * "{" * indices * "->" * body * " }"
    }
}
