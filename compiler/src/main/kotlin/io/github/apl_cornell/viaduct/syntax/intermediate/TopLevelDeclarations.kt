package io.github.apl_cornell.viaduct.syntax.intermediate

import io.github.apl_cornell.viaduct.security.LabelComponent
import io.github.apl_cornell.viaduct.security.LabelLiteral
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.DelegationKind
import io.github.apl_cornell.viaduct.syntax.DelegationProjection
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.LabelNode
import io.github.apl_cornell.viaduct.syntax.LabelVariableNode
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.ObjectVariable
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.ParameterDirection
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

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
    override val sourceLocation: SourceLocation,
) : TopLevelDeclarationNode() {

    val authority: LabelLiteral = LabelLiteral(name.value)
    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.HostDeclarationNode =
        io.github.apl_cornell.viaduct.syntax.surface.HostDeclarationNode(
            name,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): HostDeclarationNode =
        HostDeclarationNode(name, sourceLocation)
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
    override val children: Iterable<BlockNode>
        get() = listOf()

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
    val labelParameters: Arguments<LabelVariableNode>?,
    val parameters: Arguments<ParameterNode>,
    val labelConstraints: Arguments<DelegationDeclarationNode>?,
    val pcLabel: LabelNode?,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val children: Iterable<Node>
        get() = (parameters.toPersistentList() as PersistentList<Node>).add(body)

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.FunctionDeclarationNode =
        io.github.apl_cornell.viaduct.syntax.surface.FunctionDeclarationNode(
            name,
            labelParameters,
            Arguments(
                parameters.map { it.toSurfaceNode(metadata) },
                parameters.sourceLocation
            ),
            labelConstraints,
            pcLabel,
            body.toSurfaceNode(metadata),
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): Node {
        val parameters = Arguments(children.dropLast(1).map { it as ParameterNode }, parameters.sourceLocation)
        return FunctionDeclarationNode(
            name,
            labelParameters,
            parameters,
            labelConstraints,
            pcLabel,
            children.last() as BlockNode,
            sourceLocation
        )
    }

    fun getParameter(name: ObjectVariable): ParameterNode? =
        parameters.firstOrNull { param -> param.name.value == name }

    fun getParameterAtIndex(i: Int): ParameterNode? =
        if (i >= 0 && i < parameters.size) parameters[i] else null
}

/**
 * Declaration of a delegation.
 *
 */
class DelegationDeclarationNode(
    val node1: LabelNode,
    val node2: LabelNode,
    val delegationKind: DelegationKind,
    val delegationProjection: DelegationProjection,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {

    // TODO: WHERE TO WE PUT THIS?
    val congruences = {
        var node1Confidentiality: LabelComponent =
            node1.value.interpret().confidentialityComponent
        var node1Integrity: LabelComponent = node1.value.interpret().integrityComponent
        var node2Confidentiality: LabelComponent =
            node2.value.interpret().confidentialityComponent
        var node2Integrity: LabelComponent = node2.value.interpret().integrityComponent

        if (delegationKind == DelegationKind.IFC) {
            node1Confidentiality = node2Confidentiality.also { node2Confidentiality = node1Confidentiality }
        }
        node1Confidentiality = node1Confidentiality.meet(node2Confidentiality)
        node1Integrity = node1Integrity.meet(node2Integrity)

        when (delegationProjection) {
            DelegationProjection.CONFIDENTIALITY ->
                listOf(Pair(node1Confidentiality, node2Confidentiality))

            DelegationProjection.INTEGRITY ->
                listOf(Pair(node1Integrity, node2Integrity))

            DelegationProjection.BOTH ->
                listOf(
                    Pair(node1Confidentiality, node2Confidentiality),
                    Pair(node1Integrity, node2Integrity)
                )
        }
    }

    override val children: Iterable<BlockNode>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): io.github.apl_cornell.viaduct.syntax.surface.DelegationDeclarationNode =
        io.github.apl_cornell.viaduct.syntax.surface.DelegationDeclarationNode(
            node1,
            node2,
            delegationKind,
            delegationProjection,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): DelegationDeclarationNode =
        DelegationDeclarationNode(node1, node2, delegationKind, delegationProjection, sourceLocation)
}
