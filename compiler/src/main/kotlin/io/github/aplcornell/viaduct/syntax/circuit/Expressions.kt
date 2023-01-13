package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.bracketed
import io.github.aplcornell.viaduct.prettyprinting.joined
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.Operator
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.surface.keyword
import io.github.aplcornell.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node()
sealed class PureExpressionNode : ExpressionNode()
sealed class IndexExpressionNode : PureExpressionNode()

/** A literal constant. */
class LiteralNode(
    val value: Value,
    override val sourceLocation: SourceLocation,
) : IndexExpressionNode() {
    override fun toDocument(): Document = value.toDocument()
}

class ReferenceNode(
    val name: VariableNode,
    override val sourceLocation: SourceLocation,
) : IndexExpressionNode() {
    override fun toDocument(): Document = name.toDocument()
}

class LookupNode(
    private val variable: VariableNode,
    private val indices: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation,
) : PureExpressionNode() {
    override fun toDocument(): Document = variable + indices.bracketed()
}

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments<PureExpressionNode>,
    override val sourceLocation: SourceLocation,
) : PureExpressionNode() {
    override fun toDocument(): Document = Document("(") + operator.toDocument(arguments) + ")"
}

class OperatorNode(
    val operator: Operator,
    override val sourceLocation: SourceLocation,
) : Node() {
    override fun toDocument(): Document = Document("::$operator")
}

/**
 * @param defaultValue to be used when the list is empty
 * @param operator must be associative
 */
class ReduceNode(
    val operator: OperatorNode,
    val defaultValue: PureExpressionNode,
    val indices: Arguments<IndexParameterNode>,
    val body: PureExpressionNode,
    override val sourceLocation: SourceLocation,
) : PureExpressionNode() {
    override fun toDocument(): Document {
        return keyword("reduce") + listOf(operator, defaultValue).tupled() * "{" * indices.joined() * "->" * body * " }"
    }
}
