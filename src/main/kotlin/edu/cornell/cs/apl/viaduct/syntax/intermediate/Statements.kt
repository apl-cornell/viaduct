package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node() {
    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode
}

/**
 * A statement that is _not_ a combination of other statements, and that
 * does not affect control flow.
 * */
sealed class SimpleStatementNode : StatementNode() {
    abstract override val children: Iterable<ExpressionNode>

    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.SimpleStatementNode
}

/** A statement that defines a new temporary. */
interface TemporaryDefinition {
    val temporary: TemporaryNode
}

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    override val temporary: TemporaryNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), TemporaryDefinition {
    override val children: Iterable<ExpressionNode>
        get() = listOf(value)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.LetNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.LetNode(
            temporary,
            value.toSurfaceNode(),
            sourceLocation
        )
}

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    val variable: ObjectVariableNode,
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    // TODO: allow leaving out some of the labels (right now it's all or nothing)
    val labelArguments: Arguments<Located<Label>>?,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = arguments

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode(
            variable,
            className,
            typeArguments,
            labelArguments,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )
}

/** An update method applied to an object. */
class UpdateNode(
    val variable: ObjectVariableNode,
    val update: UpdateNameNode,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = arguments

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.UpdateNode(
            variable,
            update,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )
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
) : ExternalCommunicationNode(), TemporaryDefinition {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.LetNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.LetNode(
            temporary,
            edu.cornell.cs.apl.viaduct.syntax.surface.InputNode(type, host, sourceLocation),
            sourceLocation
        )
}

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExternalCommunicationNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = listOf(message)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode(
            message.toSurfaceNode(),
            host,
            sourceLocation
        )
}

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
) : InternalCommunicationNode(), TemporaryDefinition {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.LetNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.LetNode(
            temporary,
            edu.cornell.cs.apl.viaduct.syntax.surface.ReceiveNode(type, protocol, sourceLocation),
            sourceLocation
        )
}

/** Sending a value to another protocol. */
class SendNode(
    val message: AtomicExpressionNode,
    override val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : InternalCommunicationNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = listOf(message)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.SendNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.SendNode(
            message.toSurfaceNode(),
            protocol,
            sourceLocation
        )
}

// Compound Statements

/** A statement that affects control flow. */
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
) : ControlNode() {
    override val children: Iterable<Node>
        get() = listOf(guard, thenBranch, elseBranch)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.IfNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.IfNode(
            guard.toSurfaceNode(),
            thenBranch.toSurfaceNode(),
            elseBranch.toSurfaceNode(),
            sourceLocation
        )
}

/**
 * A loop that is executed until a break statement is encountered.
 *
 * @param jumpLabel A label for the loop that break nodes can refer to.
 */
class InfiniteLoopNode(
    val body: BlockNode,
    val jumpLabel: JumpLabelNode,
    override val sourceLocation: SourceLocation
) : ControlNode() {
    override val children: Iterable<BlockNode>
        get() = listOf(body)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode(
            body.toSurfaceNode(),
            jumpLabel,
            sourceLocation
        )
}

/**
 * Breaking out of a loop.
 *
 * @param jumpLabel Label of the loop to break out of. A null value refers to the innermost loop.
 */
class BreakNode(
    val jumpLabel: JumpLabelNode,
    override val sourceLocation: SourceLocation
) : ControlNode() {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode(
            jumpLabel,
            sourceLocation
        )
}

/** Asserting that a condition is true, and failing otherwise. */
class AssertionNode(
    val condition: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = listOf(condition)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.AssertionNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.AssertionNode(
            condition.toSurfaceNode(),
            sourceLocation
        )
}

/** A sequence of statements. */
class BlockNode
private constructor(
    val statements: PersistentList<StatementNode>,
    override val sourceLocation: SourceLocation
) : StatementNode(), List<StatementNode> by statements {
    constructor(statements: List<StatementNode>, sourceLocation: SourceLocation) :
        this(statements.toPersistentList(), sourceLocation)

    constructor(vararg statements: StatementNode, sourceLocation: SourceLocation) :
        this(persistentListOf(*statements), sourceLocation)

    override val children: Iterable<StatementNode>
        get() = statements

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode(
            statements.map { it.toSurfaceNode() },
            sourceLocation
        )
}
