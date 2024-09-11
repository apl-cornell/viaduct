package io.github.aplcornell.viaduct.syntax.source

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

//sealed class CircuitStatementNode : StatementNode()

sealed class CommandNode : Node()

/** A sequence of statements. */
class BlockNode<Statement : StatementNode>
private constructor(
    val statements: PersistentList<Statement>,
    val returnStatement: ReturnNode,
    override val sourceLocation: SourceLocation,
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
    override val sourceLocation: SourceLocation,
) : StatementNode() {
    override val children: Iterable<Node>
        get() = values

    override fun toDocument(): Document = keyword("return") * values.joined()
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
    val type: ArrayTypeNode,
    override val sourceLocation: SourceLocation,
) : Node(), VariableDeclarationNode {
    override val children: Iterable<Node>
        get() = listOf(type)

    override fun toDocument(): Document =
        name + Document("@") + protocol.value.toDocument() + Document(":") * type.toDocument()
}

class FunctionCallNode(
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

/** A command to create a new array on a computation protocol
 *  e.g. @Local(host = Alice) for i < 10, j < 20: a[i, j] + b[i, j]
 *  @param protocol The computation protocol on which the array is created
 *  @param indices The index variables and expressions to be used to create the array
 *  @param value The value to be assigned to each element of the array
 * */
class ArrayCreationNode(
    val protocol: ProtocolNode,
    val indices: Arguments<IndexParameterNode>,
    val value: ExpressionNode,
    override val sourceLocation: SourceLocation,
) : CommandNode() {
    override val children: Iterable<Node>
        get() = indices + listOf(value)

    override fun toDocument(): Document =
        (protocol.value.toDocument() * keyword("for") * indices.joined() + ":") * value
}
