package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.joined
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.prettyprinting.tupled
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.surface.keyword

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node()

/**
 * Declaration of a participant and their authority.
 *
 * @param name Host name.
 */
class HostDeclarationNode(
    val name: HostNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document = keyword("host") * name
}

class SizeParameterNode(
    override val name: VariableNode,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode {
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toDocument(): Document = name.toDocument()
}

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    override val name: VariableNode,
    val type: ArrayTypeNode,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode {
    override val children: Iterable<Node>
        get() = listOf(type)

    override fun toDocument(): Document =
        name + Document(":") * type.toDocument()
}

/** A simple block of statements with no control flow, all on one protocol */
class CircuitDeclarationNode(
    val name: FunctionNameNode,
    val protocol: ProtocolNode,
    val sizes: Arguments<SizeParameterNode>,
    val inputs: Arguments<ParameterNode>,
    val outputs: Arguments<ParameterNode>,
    val body: BlockNode<CircuitStatementNode>,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val children: Iterable<Node>
        get() = sizes + inputs + outputs + listOf(body)

    override fun toDocument(): Document =
        ((keyword("circuit fun") * "<" + sizes.joined() + ">") * name + "@" + protocol.value.toDocument() + inputs.tupled() * "->") * outputs.joined() * body
}

class FunctionDeclarationNode(
    val name: FunctionNameNode,
    val sizes: Arguments<SizeParameterNode>,
    val inputs: Arguments<ParameterNode>,
    val outputs: Arguments<ParameterNode>,
    val body: BlockNode<StatementNode>,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val children: Iterable<Node>
        get() = sizes + inputs + outputs + listOf(body)

    override fun toDocument(): Document =
        (keyword("fun") * "<" + sizes.joined() + ">") * name + inputs.tupled() * "->" * outputs.joined() * body
}
