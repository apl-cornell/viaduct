package io.github.apl_cornell.viaduct.syntax.intermediate

import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.LabelNode
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.ObjectVariable
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.ParameterDirection
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node() {
    abstract override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.TopLevelDeclarationNode
}

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
) : TopLevelDeclarationNode() {
    override fun children(): Iterator<Nothing> =
        iterator { }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.HostDeclarationNode =
        io.github.apl_cornell.viaduct.syntax.surface.HostDeclarationNode(
            name,
            authority,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): HostDeclarationNode =
        HostDeclarationNode(name, authority, sourceLocation)
}

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    override val name: ObjectVariableNode,
    val parameterDirection: ParameterDirection,
    val objectType: ObjectTypeNode,
    override val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : Node(), ObjectVariableDeclarationNode {
    override fun children(): Iterator<BlockNode> =
        iterator { }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.ParameterNode =
        io.github.apl_cornell.viaduct.syntax.surface.ParameterNode(
            name,
            parameterDirection,
            objectType,
            protocol,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): Node =
        ParameterNode(name, parameterDirection, objectType, protocol, sourceLocation)

    val isInParameter: Boolean
        get() = parameterDirection == ParameterDirection.IN

    val isOutParameter: Boolean
        get() = parameterDirection == ParameterDirection.OUT
}

/**
 * A function declaration associating a name with code.
 *
 * @param name A name identifying the function.
 * @param pcLabel Value of the program control label at the beginning of [body].
 * @param parameters A list of formal parameters.
 * @param body Code to run when the function is called.
 */
class FunctionDeclarationNode(
    val name: FunctionNameNode,
    val pcLabel: LabelNode?,
    val parameters: Arguments<ParameterNode>,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override fun children(): Iterator<Node> =
        iterator {
            yieldAll(parameters)
            yield(body)
        }

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.FunctionDeclarationNode =
        io.github.apl_cornell.viaduct.syntax.surface.FunctionDeclarationNode(
            name,
            pcLabel,
            Arguments(
                parameters.map { it.toSurfaceNode(metadata) },
                parameters.sourceLocation
            ),
            body.toSurfaceNode(metadata),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): Node {
        val parameters = Arguments(children.dropLast(1).map { it as ParameterNode }, parameters.sourceLocation)
        return FunctionDeclarationNode(name, pcLabel, parameters, children.last() as BlockNode, sourceLocation)
    }

    fun getParameter(name: ObjectVariable): ParameterNode? =
        parameters.firstOrNull { param -> param.name.value == name }

    fun getParameterAtIndex(i: Int): ParameterNode? =
        if (i >= 0 && i < parameters.size) parameters[i] else null
}
