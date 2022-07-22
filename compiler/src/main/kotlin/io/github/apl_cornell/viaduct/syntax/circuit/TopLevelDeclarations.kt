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

/* Non-circuit declaration
/**
 * A function declaration associating a name with code.
 *
 * @param name A name identifying the function.
 * @param pcLabel Value of the program control label at the beginning of [body].
 * @param parameters A list of formal parameters.
 * @param body Code to run when the function is called.
 */
sealed class FunctionDeclarationNode(
    open val name: FunctionNameNode,
    open val inputs: Arguments<ParameterNode>,
    open val outputs: Arguments<ParameterNode>,
    open val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode()
*/
