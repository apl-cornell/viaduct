package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.FunctionNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.JumpLabelNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.UpdateNameNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionArgumentNode as SExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.FunctionArgumentNode as SFunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.FunctionCallNode as SFunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ObjectDeclarationArgumentNode as SObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.ObjectReferenceArgumentNode as SObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.surface.OutParameterArgumentNode as SOutParameterArgumentNode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node() {
    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.StatementNode

    abstract override fun copy(children: List<Node>): StatementNode
}

/**
 * A statement that is _not_ a combination of other statements, and that does not affect
 * control flow.
 */
sealed class SimpleStatementNode : StatementNode() {
    abstract override val children: Iterable<Node>

    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.SimpleStatementNode

    abstract override fun copy(children: List<Node>): SimpleStatementNode
}

// Simple Statements

/** Binding the result of an expression to a new temporary variable. */
class LetNode(
    val temporary: TemporaryNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val children: Iterable<ExpressionNode>
        get() = listOf(value)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.LetNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.LetNode(
            temporary,
            value.toSurfaceNode(),
            sourceLocation
        )

    override fun copy(children: List<Node>): LetNode =
        LetNode(temporary, children[0] as ExpressionNode, sourceLocation)
}

/** Constructing a new object and binding it to a variable. */
class DeclarationNode(
    override val name: ObjectVariableNode,
    override val className: ClassNameNode,
    override val typeArguments: Arguments<ValueTypeNode>,
    // TODO: allow leaving out some of the labels (right now it's all or nothing)
    override val labelArguments: Arguments<Located<Label>>?,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), ObjectDeclaration {
    override val objectDeclarationAsNode: Node
        get() = this

    override val children: Iterable<AtomicExpressionNode>
        get() = arguments

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.DeclarationNode(
            name,
            className,
            typeArguments,
            labelArguments,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )

    override fun copy(children: List<Node>): DeclarationNode =
        DeclarationNode(
            name,
            className,
            typeArguments,
            labelArguments,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
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

    override fun copy(children: List<Node>): UpdateNode =
        UpdateNode(
            variable,
            update,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
            sourceLocation
        )
}

class OutParameterInitializationNode(
    val name: ObjectVariableNode,
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    val labelArguments: Arguments<Located<Label>>?,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = arguments

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.OutParameterInitializationNode =
        when (className.value) {
            ImmutableCell ->
                edu.cornell.cs.apl.viaduct.syntax.surface.OutParameterInitializationNode(
                    name,
                    arguments[0].toSurfaceNode(),
                    sourceLocation
                )

            else ->
                edu.cornell.cs.apl.viaduct.syntax.surface.OutParameterInitializationNode(
                    name,
                    edu.cornell.cs.apl.viaduct.syntax.surface.ConstructorCallNode(
                        className,
                        typeArguments,
                        labelArguments,
                        Arguments(
                            arguments.map { arg -> arg.toSurfaceNode() },
                            arguments.sourceLocation
                        ),
                        sourceLocation
                    ),
                    sourceLocation
                )
        }

    override fun copy(children: List<Node>): OutParameterInitializationNode =
        OutParameterInitializationNode(
            name,
            className,
            typeArguments,
            labelArguments,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
            sourceLocation
        )
}

sealed class FunctionArgumentNode : Node()

sealed class FunctionReturnArgumentNode : FunctionArgumentNode()

class ExpressionArgumentNode(
    val expression: AtomicExpressionNode,
    override val sourceLocation: SourceLocation
) : FunctionArgumentNode() {
    override val children: Iterable<ExpressionNode>
        get() = listOf(expression)

    override fun copy(children: List<Node>): Node =
        ExpressionArgumentNode(children[0] as AtomicExpressionNode, sourceLocation)

    override fun toSurfaceNode(): SExpressionArgumentNode =
        SExpressionArgumentNode(expression.toSurfaceNode(), sourceLocation)
}

class ObjectReferenceArgumentNode(
    val variable: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionArgumentNode() {
    override val children: Iterable<ExpressionNode>
        get() = listOf()

    override fun copy(children: List<Node>): Node =
        ObjectReferenceArgumentNode(variable, sourceLocation)

    override fun toSurfaceNode(): SObjectReferenceArgumentNode =
        SObjectReferenceArgumentNode(variable, sourceLocation)
}

class ObjectDeclarationArgumentNode(
    val variable: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionReturnArgumentNode() {
    override val children: Iterable<ExpressionNode>
        get() = listOf()

    override fun copy(children: List<Node>): Node =
        ObjectDeclarationArgumentNode(variable, sourceLocation)

    override fun toSurfaceNode(): SObjectDeclarationArgumentNode =
        SObjectDeclarationArgumentNode(variable, sourceLocation)
}

class OutParameterArgumentNode(
    val parameter: ObjectVariableNode,
    override val sourceLocation: SourceLocation
) : FunctionReturnArgumentNode() {
    override val children: Iterable<ExpressionNode>
        get() = listOf()

    override fun copy(children: List<Node>): Node =
        OutParameterArgumentNode(parameter, sourceLocation)

    override fun toSurfaceNode(): SOutParameterArgumentNode =
        SOutParameterArgumentNode(parameter, sourceLocation)
}

/** Function call. */
class FunctionCallNode(
    val name: FunctionNameNode,
    val arguments: Arguments<FunctionArgumentNode>,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode() {
    override val children: Iterable<Node>
        get() = arguments

    override fun copy(children: List<Node>): FunctionCallNode =
        FunctionCallNode(
            name,
            Arguments(
                children.map { child -> child as FunctionArgumentNode },
                arguments.sourceLocation
            ),
            sourceLocation
        )

    override fun toSurfaceNode(): SFunctionCallNode =
        SFunctionCallNode(
            name,
            Arguments(
                arguments.map { arg -> arg.toSurfaceNode() as SFunctionArgumentNode },
                arguments.sourceLocation
            ),
            sourceLocation
        )
}

// Communication Statements

/** An external output. */
class OutputNode(
    val message: AtomicExpressionNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), ExternalCommunicationNode {
    override val children: Iterable<AtomicExpressionNode>
        get() = listOf(message)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.OutputNode(
            message.toSurfaceNode(),
            host,
            sourceLocation
        )

    override fun copy(children: List<Node>): OutputNode =
        OutputNode(children[0] as AtomicExpressionNode, host, sourceLocation)
}

/** Sending a value to another protocol. */
class SendNode(
    val message: AtomicExpressionNode,
    override val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : SimpleStatementNode(), InternalCommunicationNode {
    override val children: Iterable<AtomicExpressionNode>
        get() = listOf(message)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.SendNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.SendNode(
            message.toSurfaceNode(),
            protocol,
            sourceLocation
        )

    override fun copy(children: List<Node>): SendNode =
        SendNode(children[0] as AtomicExpressionNode, protocol, sourceLocation)
}

// Compound Statements

/** A statement that affects control flow. */
sealed class ControlNode : StatementNode() {
    abstract override fun copy(children: List<Node>): ControlNode
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
    override val children: Iterable<Node>
        get() = listOf(guard, thenBranch, elseBranch)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.IfNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.IfNode(
            guard.toSurfaceNode(),
            thenBranch.toSurfaceNode(),
            elseBranch.toSurfaceNode(),
            sourceLocation
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
    override val children: Iterable<BlockNode>
        get() = listOf(body)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.InfiniteLoopNode(
            body.toSurfaceNode(),
            jumpLabel,
            sourceLocation
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
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.BreakNode(
            jumpLabel,
            sourceLocation
        )

    override fun copy(children: List<Node>): BreakNode =
        BreakNode(jumpLabel, sourceLocation)
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

    override val children: Iterable<StatementNode>
        get() = statements

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.BlockNode(
            statements.map { it.toSurfaceNode() },
            sourceLocation
        )

    override fun copy(children: List<Node>): BlockNode =
        BlockNode(children.map { it as StatementNode }, sourceLocation)
}
