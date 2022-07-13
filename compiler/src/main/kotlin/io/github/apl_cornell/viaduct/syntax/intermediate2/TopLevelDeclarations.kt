package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.LabelNode
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node()

/**
 * Declaration of a participant and their authority.
 *
 * @param name Host name.
 * @param authority Label specifying the trust placed in this host.
 */
class HostDeclarationNode(
    val name: HostNode,
    val authority: LabelNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode()

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    override val name: VariableNode,
    val objectType: ObjectTypeNode,
    override val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : Node(), VariableDeclarationNode

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

/** A simple block of statements with no control flow, all on one protocol */
class CircuitDeclarationNode(
    val name: FunctionNameNode,
    val inputs: Arguments<ParameterNode>,
    val outputs: Arguments<ParameterNode>,
    val body: CircuitBlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode()
