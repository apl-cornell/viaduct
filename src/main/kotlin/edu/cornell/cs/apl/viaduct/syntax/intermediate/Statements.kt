package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Constructor
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.Update
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    val temporary: TemporaryNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : StatementNode()

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    val variable: ObjectVariableNode,
    val constructor: Constructor,
    val arguments: Arguments,
    override val sourceLocation: SourceLocation
) : StatementNode()

/** An update method applied to an object. */
class UpdateNode(
    val variable: ObjectVariableNode,
    val update: Update,
    val arguments: Arguments,
    override val sourceLocation: SourceLocation
) : StatementNode()

// Compound Statements

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
) : StatementNode()

/**
 * A loop that is executed until a break statement is encountered.
 *
 * @param jumpLabel A label for the loop that break nodes can refer to.
 */
class InfiniteLoopNode(
    val body: BlockNode,
    val jumpLabel: JumpLabel? = null,
    override val sourceLocation: SourceLocation
) : StatementNode()

/**
 * Breaking out of a loop.
 *
 * @param jumpLabel Label of the loop to break out of. A null value refers to the innermost loop.
 */
class BreakNode(
    val jumpLabel: JumpLabel? = null,
    override val sourceLocation: SourceLocation
) : StatementNode()

/** A sequence of statements. */
class BlockNode(
    statements: List<StatementNode>,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    // Create an immutable copy
    val statements: List<StatementNode> = statements.toPersistentList()

    constructor(vararg statements: StatementNode, sourceLocation: SourceLocation) :
        this(persistentListOf(*statements), sourceLocation)
}

// Communication Statements

/**
 * An external input.
 *
 * @param temporary Store the received value to this temporary.
 * @param type Type of the value to receive.
 */
class InputNode(
    val temporary: TemporaryNode,
    val type: ValueTypeNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation
) : StatementNode()

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation
) : StatementNode()

/**
 * Receiving a value from another protocol.
 *
 * @param temporary Store the received value to this temporary.
 * @param type Type of the value to receive.
 */
class ReceiveNode(
    val temporary: TemporaryNode,
    val type: ValueTypeNode,
    val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : StatementNode()

/** Sending a value to another protocol. */
class SendNode(
    val message: AtomicExpressionNode,
    val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : StatementNode()
