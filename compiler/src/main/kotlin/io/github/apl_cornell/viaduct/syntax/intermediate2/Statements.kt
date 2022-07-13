package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.JumpLabelNode
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import kotlinx.collections.immutable.PersistentList

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
) : Node()

// Blocks

/** A sequence of statements. */
sealed class BlockNode(
    open val statements: PersistentList<StatementNode>,
    override val sourceLocation: SourceLocation
) : StatementNode(), List<StatementNode> by statements

/** A sequence of circuit statements. */
class CircuitBlockNode
private constructor(
    val statements: PersistentList<CircuitStatementNode>,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode(), List<CircuitStatementNode> by statements

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    val name: VariableNode,
    val indices: Arguments<IndexParameterNode>,
    val protocol: ProtocolNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode()

class OutParameterInitializationNode(
    val name: VariableNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : CircuitStatementNode()

// Compound Statements

/** A statement that affects control flow. */
sealed class ControlNode : StatementNode()

sealed class FunctionArgumentNode : Node()

sealed class FunctionInputArgumentNode : FunctionArgumentNode()

sealed class FunctionOutputArgumentNode : FunctionArgumentNode()

class ExpressionArgumentNode(
    val expression: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : FunctionInputArgumentNode()

class VariableReferenceArgumentNode(
    val variable: VariableNode,
    override val sourceLocation: SourceLocation
) : FunctionInputArgumentNode()

class VariableDeclarationArgumentNode(
    override val name: VariableNode,
    override val sourceLocation: SourceLocation
) : FunctionOutputArgumentNode(), VariableDeclarationNode {
    override val protocol: ProtocolNode?
        get() = null
}

class OutParameterArgumentNode(
    val parameter: VariableNode,
    override val sourceLocation: SourceLocation
) : FunctionOutputArgumentNode()

/** Function call. */
class FunctionCallNode(
    val name: FunctionNameNode,
    val arguments: Arguments<FunctionArgumentNode>,
    override val sourceLocation: SourceLocation
) : ControlNode()

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
