package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.braced
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.concatenated
import io.github.apl_cornell.viaduct.prettyprinting.joined
import io.github.apl_cornell.viaduct.prettyprinting.nested
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.prettyprinting.tupled
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.surface.keyword
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A computation with side effects. */
sealed class StatementNode : Node()

sealed class CircuitStatementNode : StatementNode()

sealed class CommandNode : Node()

/** A sequence of statements. */
class BlockNode<Statement : StatementNode>
private constructor(
    val statements: PersistentList<Statement>,
    val returnStatement: ReturnNode,
    override val sourceLocation: SourceLocation
) : Node(), List<Statement> by statements {
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
    override val sourceLocation: SourceLocation
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
    override val sourceLocation: SourceLocation
) : CircuitStatementNode(), VariableDeclarationNode {
    override val children: Iterable<Node>
        get() = indices + listOf(type, value)

    override fun toDocument(): Document =
        keyword("val") * name + indices.bracketed() * "=" * value
}

class LetNode(
    val bindings: Arguments<VariableBindingNode>,
    val command: CommandNode,
    override val sourceLocation: SourceLocation
) : StatementNode() {
    override val children: Iterable<Node>
        get() = bindings + listOf(command)

    override fun toDocument(): Document =
        keyword("val") * bindings.joined() * "=" * command
}

class VariableBindingNode(
    override val name: VariableNode,
    val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
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
    override val sourceLocation: SourceLocation
) : CommandNode() {
    override val children: Iterable<Node>
        get() = bounds + inputs

    override fun toDocument(): Document {
        return name + bounds.joined(
            prefix = Document("<"),
            postfix = Document(">")
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
    override val sourceLocation: SourceLocation
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
    override val sourceLocation: SourceLocation
) : CommandNode() {
    override val children: Iterable<Node>
        get() = listOf(message)

    override fun toDocument(): Document = host + "." + keyword("output") + listOf(message).tupled()
}
