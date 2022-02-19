package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectTypeNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ParameterDirection
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node() {
    abstract override fun toSurfaceNode(metadata: Metadata): edu.cornell.cs.apl.viaduct.syntax.surface.TopLevelDeclarationNode
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
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode(
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
    override val objectType: ObjectTypeNode,
    override val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : Node(), ObjectDeclaration {
    override val declarationAsNode: Node
        get() = this

    override val children: Iterable<BlockNode>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): edu.cornell.cs.apl.viaduct.syntax.surface.ParameterNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ParameterNode(
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
    override val children: Iterable<Node>
        get() = (parameters.toPersistentList() as PersistentList<Node>).add(body)

    override fun toSurfaceNode(metadata: Metadata): edu.cornell.cs.apl.viaduct.syntax.surface.FunctionDeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.FunctionDeclarationNode(
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
