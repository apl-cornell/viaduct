package io.github.aplcornell.viaduct.syntax.circuit

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
import io.github.aplcornell.viaduct.syntax.FunctionNameNode
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.surface.keyword
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

sealed class CircuitStatementNode : StatementNode()

sealed class CommandNode : Node()

/** Any sequence of statements */
abstract class BlockNode<Statement : StatementNode>(
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
) : StatementNode() {
    override val children: Iterable<Node>
        get() = values

    override fun toDocument(): Document = keyword("return") * values.joined()
}

/**
 * Binding the result of an expression to a variable.
 * Note that scalars are represented as arrays of dimension zero:
 *     val x = 5 ===> val x[] = 5
 */
class CircuitLetNode(
    override val name: VariableNode,
    val indices: Arguments<IndexParameterNode>,
    val type: ArrayTypeNode,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation,
) : CircuitStatementNode(), VariableDeclarationNode {
    override val children: Iterable<Node>
        get() = indices + listOf(type, value)

    override fun toDocument(): Document =
        (keyword("val") * name + indices.bracketed() + ":") * type * "=" * value
}

class LetNode(
    val bindings: Arguments<VariableBindingNode>,
    val command: CommandNode,
    override val sourceLocation: SourceLocation,
) : StatementNode() {
    override val children: Iterable<Node>
        get() = bindings + listOf(command)

    override fun toDocument(): Document =
        keyword("val") * bindings.joined() * "=" * command
}

class VariableBindingNode(
    override val name: VariableNode,
    val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation,
) : Node(), VariableDeclarationNode {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document =
        name + "@" + protocol
}

class CircuitCallNode(
    val name: FunctionNameNode,
    val bounds: Arguments<IndexExpressionNode>,
    val inputs: Arguments<ReferenceNode>,
    override val sourceLocation: SourceLocation,
) : CommandNode() {
    override val children: Iterable<Node>
        get() = bounds + inputs

    override fun toDocument(): Document {
        return name + bounds.joined(
            prefix = Document("<"),
            postfix = Document(">"),
        ) + inputs.tupled()
    }
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

/** A command that affects control flow. */
sealed class ControlNode : CommandNode()

/**
 * Executing statements conditionally.
 *
 * @param thenBranch Statements to execute if the guard is true.
 * @param elseBranch Statements to execute if the guard is false.
 */
class IfNode(
    val guard: IndexExpressionNode,
    val thenBranch: BlockNode<StatementNode>,
    val elseBranch: BlockNode<StatementNode>,
    override val sourceLocation: SourceLocation,
) : ControlNode() {
    override val children: Iterable<Node>
        get() = listOf(guard, thenBranch, elseBranch)

    override fun toDocument(): Document =
        keyword("if") + listOf(guard).tupled() + thenBranch.toDocument() + keyword("else") + elseBranch.toDocument()
}

/**
 * A loop that is executed until a break statement is encountered.
 *
 * @param jumpLabel A label for the loop that break nodes can refer to.
 */
class LoopNode(
    val body: BlockNode<StatementNode>,
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
