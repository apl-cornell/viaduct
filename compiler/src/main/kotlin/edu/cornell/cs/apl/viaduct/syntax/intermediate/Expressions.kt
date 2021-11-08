package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.QueryNameNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.TemporaryNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/** A computation that produces a result. */
sealed class ExpressionNode : Node() {
    abstract override val children: Iterable<AtomicExpressionNode>

    final override fun toSurfaceNode(metadata: Metadata): edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode =
        toSurfaceNode()

    abstract fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ExpressionNode

    abstract override fun copy(children: List<Node>): ExpressionNode
}

sealed class PureExpressionNode : ExpressionNode()

/** An expression that requires no computation to reduce to a value. */
sealed class AtomicExpressionNode : PureExpressionNode() {
    final override val children: Iterable<Nothing>
        get() = listOf()

    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.AtomicExpressionNode

    abstract override fun copy(children: List<Node>): AtomicExpressionNode
}

/** A literal constant. */
class LiteralNode(val value: Value, override val sourceLocation: SourceLocation) :
    AtomicExpressionNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.LiteralNode(value, sourceLocation)

    override fun copy(children: List<Node>): LiteralNode =
        LiteralNode(value, sourceLocation)
}

/** Reading the value stored in a temporary. */
class ReadNode(val temporary: TemporaryNode) : AtomicExpressionNode() {
    override val sourceLocation: SourceLocation
        get() = temporary.sourceLocation

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ReadNode(temporary)

    override fun copy(children: List<Node>): ReadNode =
        ReadNode(temporary)
}

/** An n-ary operator applied to n arguments. */
class OperatorApplicationNode(
    val operator: Operator,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : PureExpressionNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = arguments

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.OperatorApplicationNode(
            operator,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )

    override fun copy(children: List<Node>): OperatorApplicationNode =
        OperatorApplicationNode(
            operator,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
            sourceLocation
        )
}

/** A query method applied to an object. */
class QueryNode(
    val variable: ObjectVariableNode,
    val query: QueryNameNode,
    val arguments: Arguments<AtomicExpressionNode>,
    override val sourceLocation: SourceLocation
) : PureExpressionNode() {
    override val children: Iterable<AtomicExpressionNode>
        get() = arguments

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.QueryNode(
            variable,
            query,
            Arguments(arguments.map { it.toSurfaceNode() }, arguments.sourceLocation),
            sourceLocation
        )

    override fun copy(children: List<Node>): QueryNode =
        QueryNode(
            variable,
            query,
            Arguments(children.map { it as AtomicExpressionNode }, arguments.sourceLocation),
            sourceLocation
        )
}

/** Reducing the confidentiality or increasing the integrity of the result of an expression. */
// TODO: downgrades are very much not pure.
sealed class DowngradeNode : PureExpressionNode() {
    /** Expression whose label is being downgraded. */
    abstract val expression: AtomicExpressionNode

    /** The label [expression] must have before the downgrade. */
    abstract val fromLabel: LabelNode?

    /** The label after the downgrade. */
    abstract val toLabel: LabelNode?

    final override val children: Iterable<AtomicExpressionNode>
        get() = listOf(expression)

    abstract override fun copy(children: List<Node>): DowngradeNode
}

/** Revealing the result of an expression (reducing confidentiality). */
class DeclassificationNode(
    override val expression: AtomicExpressionNode,
    override val fromLabel: LabelNode?,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation
) : DowngradeNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.DeclassificationNode(
            expression.toSurfaceNode(),
            fromLabel,
            toLabel,
            sourceLocation
        )

    override fun copy(children: List<Node>): DeclassificationNode =
        DeclassificationNode(children[0] as AtomicExpressionNode, fromLabel, toLabel, sourceLocation)
}

/** Trusting the result of an expression (increasing integrity). */
class EndorsementNode(
    override val expression: AtomicExpressionNode,
    override val fromLabel: LabelNode,
    override val toLabel: LabelNode?,
    override val sourceLocation: SourceLocation
) : DowngradeNode() {
    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.EndorsementNode(
            expression.toSurfaceNode(),
            fromLabel,
            toLabel,
            sourceLocation
        )

    override fun copy(children: List<Node>): EndorsementNode =
        EndorsementNode(children[0] as AtomicExpressionNode, fromLabel, toLabel, sourceLocation)
}

// Communication Expressions

/**
 * An external input.
 *
 * @param type Type of the value to receive.
 */
class InputNode(
    val type: ValueTypeNode,
    override val host: HostNode,
    override val sourceLocation: SourceLocation
) : ExpressionNode(), CommunicationNode {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.InputNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.InputNode(type, host, sourceLocation)

    override fun copy(children: List<Node>): InputNode =
        InputNode(type, host, sourceLocation)
}
