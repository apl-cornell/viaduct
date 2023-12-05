package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.braced
import io.github.aplcornell.viaduct.prettyprinting.bracketed
import io.github.aplcornell.viaduct.prettyprinting.concatenated
import io.github.aplcornell.viaduct.prettyprinting.joined
import io.github.aplcornell.viaduct.prettyprinting.nested
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.LabelNode
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.surface.keyword
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

sealed class StatementNode : Node()

sealed class CommandNode : Node()

/** A statement that affects control flow. */
sealed class ControlNode : StatementNode()

/** A statement that binds a variable. */
sealed class LetNode : StatementNode(), VariableDeclarationNode

/** Any sequence of statements */
sealed class BlockNode<Statement : StatementNode>(
    open val statements: List<Statement>,
    override val sourceLocation: SourceLocation,
) : Node(), List<Statement>

/** A sequence of statements not followed by a return. */
class ControlFlowBlockNode<Statement : StatementNode>
private constructor(
    statements: PersistentList<Statement>,
    sourceLocation: SourceLocation,
) : BlockNode<Statement>(statements = statements, sourceLocation = sourceLocation), List<Statement> by statements {
    constructor(statements: List<Statement>, sourceLocation: SourceLocation) :
        this(statements.toPersistentList(), sourceLocation)

    override val children: Iterable<Node>
        get() = statements

    override fun toDocument(): Document {
        val statements: MutableList<Document> = (statements.map { it.toDocument() } as MutableList<Document>)
        val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
        return listOf((Document.forcedLineBreak + body).nested() + Document.forcedLineBreak).braced()
    }
}

/** A sequence of statements followed by a return. */
class RoutineBlockNode<Statement : StatementNode>
private constructor(
    statements: PersistentList<Statement>,
    val returnStatement: ReturnNode,
    sourceLocation: SourceLocation,
) : BlockNode<Statement>(statements = statements, sourceLocation = sourceLocation), List<Statement> by statements {
    constructor(statements: List<Statement>, returnStatement: ReturnNode, sourceLocation: SourceLocation) :
        this(statements.toPersistentList(), returnStatement, sourceLocation)

    override val children: Iterable<Node>
        get() = statements + listOf(returnStatement)

    override fun toDocument(): Document {
        val statements: MutableList<Document> = (statements.map { it.toDocument() } as MutableList<Document>)
        statements.add(returnStatement.toDocument())
        val body: Document = statements.concatenated(separator = Document.forcedLineBreak)
        return listOf((Document.forcedLineBreak + body).nested() + Document.forcedLineBreak).braced()
    }
}

class ReturnNode(
    val values: Arguments<ReferenceNode>,
    override val sourceLocation: SourceLocation,
) : ControlNode() {
    override val children: Iterable<Node>
        get() = values

    override fun toDocument(): Document = keyword("return") * values.joined()
}

/**
 * Binding the result of an expression to a variable.
 * Note that scalars are represented as arrays of dimension zero:
 *     val x = 5 ===> val x[] = 5
 */
class ComputeLetNode(
    override val name: VariableNode,
    val indices: Arguments<IndexParameterNode>,
    val type: ArrayTypeNode,
    val protocol: ProtocolNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation,
) : LetNode() {
    override val children: Iterable<Node>
        get() = indices + listOf(type, value)

    override fun toDocument(): Document =
        (keyword("val") * name + indices.bracketed() + ":") * type + "@" + protocol * "=" * value
}

class CommandLetNode(
    override val name: VariableNode,
    val protocol: ProtocolNode,
    val command: CommandNode,
    override val sourceLocation: SourceLocation,
) : LetNode() {
    override val children: Iterable<Node>
        get() = listOf(command)

    override fun toDocument(): Document = // TODO fix me, compile errors, and parsing
        keyword("val") * name + "@" + protocol * "=" * command
}

/**
 * An external input.
 * @param type Type of the value to receive.
 */
class InputNode(
    val type: ArrayTypeNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation,
) : CommandNode() {
    override val children: Iterable<Node>
        get() = listOf(type)

    override fun toDocument(): Document = host + "." + keyword("input") + "<" + type + ">()"
}

/** An external output. */
class OutputNode(
    val type: ArrayTypeNode,
    val message: ReferenceNode,
    val host: HostNode,
    override val sourceLocation: SourceLocation,
) : CommandNode() {
    override val children: Iterable<Node>
        get() = listOf(message)

    override fun toDocument(): Document = host + "." + keyword("output") + listOf(message).tupled()
}

/**
 * Executing statements conditionally.
 *
 * @param thenBranch Statements to execute if the guard is true.
 * @param elseBranch Statements to execute if the guard is false.
 */
class IfNode(
    val guard: IndexExpressionNode,
    val thenBranch: ControlFlowBlockNode<StatementNode>,
    val elseBranch: ControlFlowBlockNode<StatementNode>,
    override val sourceLocation: SourceLocation,
) : ControlNode() {
    override val children: Iterable<Node>
        get() = listOf(guard, thenBranch, elseBranch)

    override fun toDocument(): Document =
        keyword("if") + listOf(guard).tupled() + thenBranch.toDocument() * keyword("else") + elseBranch.toDocument()
}

/**
 * A loop that is executed until a break statement is encountered.
 *
 * @param jumpLabel A label for the loop that break nodes can refer to.
 */
class LoopNode(
    val body: ControlFlowBlockNode<StatementNode>,
//    val jumpLabel: JumpLabelNode?,
    override val sourceLocation: SourceLocation,
) : ControlNode() {
    override val children: Iterable<Node>
        get() = listOf(body)

    override fun toDocument(): Document = keyword("loop") + body.toDocument()
}

/**
 * Breaking out of a loop.
 *
 * @param jumpLabel Label of the loop to break out of. A null value refers to the innermost loop.
 */
class BreakNode(
//    val jumpLabel: JumpLabelNode,
    override val sourceLocation: SourceLocation,
) : ControlNode() {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document = keyword("break")
}

/** Reducing the confidentiality or increasing the integrity of the result of an expression. */
sealed class DowngradeNode : CommandNode() {
    /** Expression whose label is being downgraded. */
    abstract val expression: ReferenceNode

    /** The label [expression] must have before the downgrade. */
    abstract val fromLabel: LabelNode?

    /** The label after the downgrade. */
    abstract val toLabel: LabelNode?

    final override val children: Iterable<Node>
        get() = listOf(expression)
}

/** Revealing the result of an expression (reducing confidentiality). */
class DeclassificationNode(
    override val expression: ReferenceNode,
    override val fromLabel: LabelNode?,
    override val toLabel: LabelNode,
    override val sourceLocation: SourceLocation,
) : DowngradeNode() {
    override fun toDocument(): Document {
        val from = fromLabel?.let { Document() * keyword("from") * listOf(it).braced() } ?: Document()
        val to = keyword("to") * listOf(toLabel).braced()
        return keyword("declassify") * expression + from * to
    }
}

/** Trusting the result of an expression (increasing integrity). */
class EndorsementNode(
    override val expression: ReferenceNode,
    override val fromLabel: LabelNode,
    override val toLabel: LabelNode?,
    override val sourceLocation: SourceLocation,
) : DowngradeNode() {
    override fun toDocument(): Document {
        val from = keyword("from") * listOf(fromLabel).braced()
        val to = toLabel?.let { Document() * keyword("to") * listOf(it).braced() } ?: Document()
        return keyword("endorse") * expression + to * from
    }
}
