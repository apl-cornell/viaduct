package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.concatenated
import io.github.apl_cornell.viaduct.prettyprinting.joined
import io.github.apl_cornell.viaduct.prettyprinting.nested
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.VariableNode
import io.github.apl_cornell.viaduct.syntax.surface.keyword
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

/**
 * A statement that is _not_ a combination of other statements, and that does not affect
 * control flow.
 */
sealed class SimpleStatementNode : StatementNode()

sealed class CircuitStatementNode : SimpleStatementNode()

class IndexParameterNode(
    val name: VariableNode,
    val bound: IndexExpressionNode,
    override val sourceLocation: SourceLocation
) : Node() {
    override fun toDocument(): Document = name.toDocument() * "<" * bound
}

// Blocks

/* Non-circuit statement
/** A sequence of statements. */
sealed class BlockNode(
    open val statements: PersistentList<StatementNode>,
    override val sourceLocation: SourceLocation
) : StatementNode(), List<StatementNode> by statements {
    override fun toDocument(): Document {
        val statements: List<Document> = statements.map {
            if (it is SimpleStatementNode)
                it.toDocument() + ";"
            else
                it.toDocument()
        }
        val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
        return Document("{") +
            (Document.forcedLineBreak + body).nested() + Document.forcedLineBreak + "}"
    }
}
 */

/** A sequence of circuit statements. */
class CircuitBlockNode
private constructor(
    val statements: PersistentList<CircuitStatementNode>,
    val returnStatement: ReturnNode?,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode(), List<CircuitStatementNode> by statements {
    constructor(statements: List<CircuitStatementNode>, returnStatement: ReturnNode?, sourceLocation: SourceLocation) :
        this(statements.toPersistentList(), returnStatement, sourceLocation)

    override fun toDocument(): Document {
        val statements: MutableList<Document> = (statements.map { it.toDocument() + ";" } as MutableList<Document>)
        if (returnStatement != null) statements.add(returnStatement.toDocument())
        val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
        return Document("{") +
            (Document.forcedLineBreak + body).nested() + Document.forcedLineBreak + "}"
    }
}

// Simple Statements

/** Binding the result of an expression to a variable. */
class LetNode(
    val name: VariableNode,
    val indices: Arguments<IndexParameterNode>,
    val protocol: ProtocolNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode() {
    override fun toDocument(): Document =
        keyword("val") * name + indices.bracketed() + "@" + protocol.value.toDocument() * "=" * value
}

class ReturnNode(
    val values: Arguments<PureExpressionNode>,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode() {
    override fun toDocument(): Document = keyword("return") * values.joined()
}

/* Non-circuit statements

class OutParameterInitializationNode(
    val name: VariableNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode() {
    override fun toDocument(): Document = keyword("out") * name * Document("=") * value
}

// Compound Statements

/** A statement that affects control flow. */
sealed class ControlNode : StatementNode()

sealed class FunctionArgumentNode : Node()

sealed class FunctionInputArgumentNode : FunctionArgumentNode()

sealed class FunctionOutputArgumentNode : FunctionArgumentNode()

class ExpressionArgumentNode(
    val expression: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : FunctionInputArgumentNode() {
    override fun toDocument(): Document = expression.toDocument()
}

class ArrayReferenceArgumentNode(
    val variable: VariableNode,
    override val sourceLocation: SourceLocation
) : FunctionInputArgumentNode() {
    override fun toDocument(): Document = Document("&${variable.value.name}")
}

class VariableDeclarationArgumentNode(
    override val variable: VariableNode,
    override val sourceLocation: SourceLocation
) : FunctionOutputArgumentNode(), VariableDeclarationNode {
    override val protocol: ProtocolNode?
        get() = null

    override fun toDocument(): Document = keyword("val") * Document(variable.value.name)
}

class OutParameterArgumentNode(
    val parameter: VariableNode,
    override val sourceLocation: SourceLocation
) : FunctionOutputArgumentNode() {
    override fun toDocument(): Document = keyword("out") * Document(parameter.value.name)
}

/** Function call. */
class FunctionCallNode(
    val name: FunctionNameNode,
    val arguments: Arguments<FunctionArgumentNode>,
    override val sourceLocation: SourceLocation
) : ControlNode() {
    override fun toDocument(): Document = name + arguments.tupled()
}

/**
 * Executing statements conditionally.
 *
 * @param thenBranch Statement to execute if the guard is true.
 * @param elseBranch Statement to execute if the guard is false.
 */
class IfNode(
    val guard: AtomicExpressionNode,
    val thenBranch: BlockNode,
    val elseBranch: BlockNode,
    override val sourceLocation: SourceLocation
) : ControlNode()

/**
 * A loop that is executed until a break statement is encountered.
 *
 * @param jumpLabel A label for the loop that break nodes can refer to.
 */
class InfiniteLoopNode(
    val body: BlockNode,
    val jumpLabel: JumpLabelNode,
    override val sourceLocation: SourceLocation
) : ControlNode()

/**
 * Breaking out of a loop.
 *
 * @param jumpLabel Label of the loop to break out of. A null value refers to the innermost loop.
 */
class BreakNode(
    val jumpLabel: JumpLabelNode,
    override val sourceLocation: SourceLocation
) : ControlNode()

/** Asserting that a condition is true, and failing otherwise. */
class AssertionNode(
    val condition: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : StatementNode()
*/
