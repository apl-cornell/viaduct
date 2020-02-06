package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Constructor
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
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

// TODO: remove this? This was only necessary for ForNodes.
//   Only reason to keep would be reverse elaboration.
sealed class SimpleStatementNode : StatementNode()

/** A statement that defines a new temporary name. */
// TODO: better name
interface TemporaryBindingForm {
    val temporary: TemporaryNode
}

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    override val temporary: TemporaryNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), TemporaryBindingForm

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    val variable: ObjectVariableNode,
    val constructor: Constructor,
    val arguments: Arguments,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode()

/** An update method applied to an object. */
class UpdateNode(
    val variable: ObjectVariableNode,
    val update: Update,
    val arguments: Arguments,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode()

// Compound Statements

// TODO: remove this.
sealed class ControlNode : StatementNode()

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
) : StatementNode()

/** A sequence of statements. */
class BlockNode(
    statements: List<StatementNode>,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    // Make an immutable copy
    val statements: List<StatementNode> = statements.toPersistentList()

    constructor(vararg statements: StatementNode, sourceLocation: SourceLocation) :
        this(persistentListOf(*statements), sourceLocation)

    fun singletonStatement(): StatementNode? {
        return if (statements.size == 1) statements[0] else null
    }
}

// Communication Statements

/** A node for sending or receiving messages. */
sealed class CommunicationNode : SimpleStatementNode()

/** Communication happening between a protocol and a host. */
sealed class ExternalCommunicationNode : CommunicationNode() {
    abstract val host: HostNode
}

/** Communication happening between protocols. */
sealed class InternalCommunicationNode : CommunicationNode() {
    abstract val protocol: ProtocolNode
}

/**
 * An external input.
 *
 * @param temporary Store the received value to this temporary.
 * @param type Type of the value to receive.
 */
class InputNode(
    override val temporary: TemporaryNode,
    val type: ValueTypeNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExternalCommunicationNode(), TemporaryBindingForm

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExternalCommunicationNode()

/**
 * Receiving a value from another protocol.
 *
 * @param temporary Store the received value to this temporary.
 * @param type Type of the value to receive.
 */
class ReceiveNode(
    override val temporary: TemporaryNode,
    val type: ValueTypeNode,
    override val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : InternalCommunicationNode(), TemporaryBindingForm

/** Sending a value to another protocol. */
class SendNode(
    val message: AtomicExpressionNode,
    override val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : InternalCommunicationNode()
