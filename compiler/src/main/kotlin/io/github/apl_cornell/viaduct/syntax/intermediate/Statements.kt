package io.github.apl_cornell.viaduct.syntax.intermediate

import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.JumpLabelNode
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.TemporaryNode
import io.github.apl_cornell.viaduct.syntax.UpdateNameNode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node() {
    abstract override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.StatementNode

    abstract override fun copy(children: List<Node>): StatementNode
}

/**
 * A statement that is _not_ a combination of other statements, and that does not affect
 * control flow.
 */
sealed class SimpleStatementNode : StatementNode() {
    abstract override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.SimpleStatementNode

    abstract override fun copy(children: List<Node>): SimpleStatementNode
}

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    override val name: TemporaryNode,
    val value: ExpressionNode,
    override val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), VariableDeclarationNode {
    override fun children(): Iterator<ExpressionNode> =
        iterator { yield(value) }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.LetNode =
        io.github.apl_cornell.viaduct.syntax.surface.LetNode(
            name,
            value.toSurfaceNode(),
            protocol,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): LetNode =
        LetNode(name, children[0] as ExpressionNode, protocol, sourceLocation)
}

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    override val name: ObjectVariableNode,
    val objectType: ObjectTypeNode,
    val arguments: Arguments<AtomicExpressionNode>,
    override val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), ObjectVariableDeclarationNode {
    override fun children(): Iterator<AtomicExpressionNode> =
        arguments.iterator()

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.DeclarationNode =
        io.github.apl_cornell.viaduct.syntax.surface.DeclarationNode(
            name,
            io.github.apl_cornell.viaduct.syntax.surface.ConstructorCallNode(
                objectType,
                protocol,
                Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
                sourceLocation
            ),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): DeclarationNode =
        DeclarationNode(
            name,
            objectType,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
            protocol,
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
    override fun children(): Iterator<AtomicExpressionNode> =
        arguments.iterator()

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.UpdateNode =
        io.github.apl_cornell.viaduct.syntax.surface.UpdateNode(
            variable,
            update,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): UpdateNode =
        UpdateNode(
            variable,
            update,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
            sourceLocation
        )
}

sealed class OutParameterInitializerNode : Node() {
    final override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.ExpressionNode =
        toSurfaceNode()

    abstract fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.ExpressionNode
}

class OutParameterExpressionInitializerNode(
    val expression: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : OutParameterInitializerNode() {
    override fun children(): Iterator<AtomicExpressionNode> =
        iterator { yield(expression) }

    override fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.AtomicExpressionNode =
        expression.toSurfaceNode()

    override fun copy(children: List<Node>): Node =
        OutParameterExpressionInitializerNode(
            children[0] as AtomicExpressionNode,
            sourceLocation
        )
}

class OutParameterConstructorInitializerNode(
    val objectType: ObjectTypeNode,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : OutParameterInitializerNode() {
    override fun children(): Iterator<AtomicExpressionNode> =
        arguments.iterator()

    override fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.ConstructorCallNode =
        io.github.apl_cornell.viaduct.syntax.surface.ConstructorCallNode(
            objectType,
            null,
            Arguments(
                arguments.map { arg -> arg.toSurfaceNode() },
                arguments.sourceLocation
            ),
            sourceLocation
        )

    override fun copy(children: List<Node>): OutParameterConstructorInitializerNode =
        OutParameterConstructorInitializerNode(
            objectType,
            Arguments(
                children.map { child -> child as AtomicExpressionNode },
                arguments.sourceLocation
            ),
            sourceLocation
        )
}

class OutParameterInitializationNode(
    val name: ObjectVariableNode,
    val initializer: OutParameterInitializerNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override fun children(): Iterator<OutParameterInitializerNode> =
        iterator { yield(initializer) }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.OutParameterInitializationNode =
        io.github.apl_cornell.viaduct.syntax.surface.OutParameterInitializationNode(
            name,
            initializer.toSurfaceNode(),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): OutParameterInitializationNode =
        OutParameterInitializationNode(
            name,
            children[0] as OutParameterInitializerNode,
            sourceLocation
        )
}
// Communication Statements

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), CommunicationNode {
    override fun children(): Iterator<AtomicExpressionNode> =
        iterator { yield(message) }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.OutputNode =
        io.github.apl_cornell.viaduct.syntax.surface.OutputNode(
            message.toSurfaceNode(),
            host,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): OutputNode =
        OutputNode(children[0] as AtomicExpressionNode, host, sourceLocation)
}

// Compound Statements

/** A statement that affects control flow. */
sealed class ControlNode : StatementNode() {
    abstract override fun copy(children: List<Node>): ControlNode
}

sealed class FunctionArgumentNode : Node() {
    final override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.FunctionArgumentNode =
        toSurfaceNode()

    abstract fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.FunctionArgumentNode
}

sealed class FunctionInputArgumentNode : FunctionArgumentNode()

sealed class FunctionOutputArgumentNode : FunctionArgumentNode()

class ExpressionArgumentNode(
    val expression: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : FunctionInputArgumentNode() {
    override fun children(): Iterator<ExpressionNode> =
        iterator { yield(expression) }

    override fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.ExpressionArgumentNode =
        io.github.apl_cornell.viaduct.syntax.surface.ExpressionArgumentNode(expression.toSurfaceNode(), sourceLocation)

    override fun copy(children: List<Node>): Node =
        ExpressionArgumentNode(children[0] as AtomicExpressionNode, sourceLocation)
}

class ObjectReferenceArgumentNode(
    val variable: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionInputArgumentNode() {
    override fun children(): Iterator<ExpressionNode> =
        iterator { }

    override fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.ObjectReferenceArgumentNode =
        io.github.apl_cornell.viaduct.syntax.surface.ObjectReferenceArgumentNode(variable, sourceLocation)

    override fun copy(children: List<Node>): Node =
        ObjectReferenceArgumentNode(variable, sourceLocation)
}

class ObjectDeclarationArgumentNode(
    override val name: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionOutputArgumentNode(), ObjectVariableDeclarationNode {
    override val protocol: ProtocolNode?
        get() = null

    override fun children(): Iterator<ExpressionNode> =
        iterator { }

    override fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.ObjectDeclarationArgumentNode =
        io.github.apl_cornell.viaduct.syntax.surface.ObjectDeclarationArgumentNode(name, sourceLocation)

    override fun copy(children: List<Node>): Node =
        ObjectDeclarationArgumentNode(name, sourceLocation)
}

class OutParameterArgumentNode(
    val parameter: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionOutputArgumentNode() {
    override fun children(): Iterator<ExpressionNode> =
        iterator { }

    override fun toSurfaceNode(): io.github.apl_cornell.viaduct.syntax.surface.OutParameterArgumentNode =
        io.github.apl_cornell.viaduct.syntax.surface.OutParameterArgumentNode(parameter, sourceLocation)

    override fun copy(children: List<Node>): Node =
        OutParameterArgumentNode(parameter, sourceLocation)
}

/** Function call. */
class FunctionCallNode(
    val name: FunctionNameNode,
    val arguments: Arguments<FunctionArgumentNode>,
    override val sourceLocation: SourceLocation
) : ControlNode() {
    override fun children(): Iterator<Node> =
        arguments.iterator()

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.FunctionCallNode =
        io.github.apl_cornell.viaduct.syntax.surface.FunctionCallNode(
            name,
            Arguments(
                arguments.map { arg -> arg.toSurfaceNode() },
                arguments.sourceLocation
            ),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): FunctionCallNode =
        FunctionCallNode(
            name,
            Arguments(
                children.map { child -> child as FunctionArgumentNode },
                arguments.sourceLocation
            ),
            sourceLocation
        )
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
) : ControlNode() {
    override fun children(): Iterator<Node> =
        iterator {
            yield(guard)
            yield(thenBranch)
            yield(elseBranch)
        }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.IfNode =
        io.github.apl_cornell.viaduct.syntax.surface.IfNode(
            guard.toSurfaceNode(),
            thenBranch.toSurfaceNode(metadata),
            elseBranch.toSurfaceNode(metadata),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): IfNode =
        IfNode(children[0] as AtomicExpressionNode, children[1] as BlockNode, children[2] as BlockNode, sourceLocation)
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
    override fun children(): Iterator<BlockNode> =
        iterator { yield(body) }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.InfiniteLoopNode =
        io.github.apl_cornell.viaduct.syntax.surface.InfiniteLoopNode(
            body.toSurfaceNode(metadata),
            jumpLabel,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): InfiniteLoopNode =
        InfiniteLoopNode(children[0] as BlockNode, jumpLabel, sourceLocation)
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
    override fun children(): Iterator<Nothing> =
        iterator { }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.BreakNode =
        io.github.apl_cornell.viaduct.syntax.surface.BreakNode(
            jumpLabel,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): BreakNode =
        BreakNode(jumpLabel, sourceLocation)
}

/** Asserting that a condition is true, and failing otherwise. */
class AssertionNode(
    val condition: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    override fun children(): Iterator<AtomicExpressionNode> =
        iterator { yield(condition) }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.AssertionNode =
        io.github.apl_cornell.viaduct.syntax.surface.AssertionNode(
            condition.toSurfaceNode(),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): AssertionNode =
        AssertionNode(children[0] as AtomicExpressionNode, sourceLocation)
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

    override fun children(): Iterator<StatementNode> =
        statements.iterator()

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.BlockNode =
        io.github.apl_cornell.viaduct.syntax.surface.BlockNode(
            statements.map { it.toSurfaceNode(metadata) },
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): BlockNode =
        BlockNode(children.map { it as StatementNode }, sourceLocation)
}
