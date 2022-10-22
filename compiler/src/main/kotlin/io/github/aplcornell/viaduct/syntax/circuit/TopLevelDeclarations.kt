package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.joined
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.ArrayTypeNode
import io.github.aplcornell.viaduct.syntax.FunctionNameNode
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import io.github.aplcornell.viaduct.syntax.surface.keyword

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
    override fun toDocument(): Document = keyword("host") * name
}

class BoundParameterNode(
    override val name: VariableNode,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode {
    override fun toDocument(): Document = name.toDocument()
}

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    override val name: VariableNode,
    val type: ArrayTypeNode,
    val protocol: ProtocolNode,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode {
    override fun toDocument(): Document =
        name + Document(":") * type.toDocument() + Document("@") + protocol.value
}

/** A simple block of statements with no control flow, all on one protocol */
class CircuitDeclarationNode(
    val name: FunctionNameNode,
    val protocol: ProtocolNode,
    val bounds: Arguments<BoundParameterNode>,
    val inputs: Arguments<ParameterNode>,
    val outputs: Arguments<ParameterNode>,
    val body: CircuitBlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override fun toDocument(): Document =
        ((keyword("circuit fun") * "<" + bounds.joined() + ">") * name + "@" + protocol.value.toDocument() + inputs.tupled() * "->") * outputs.joined() * body
}
