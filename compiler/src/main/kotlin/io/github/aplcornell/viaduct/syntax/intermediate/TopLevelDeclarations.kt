package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.passes.PrincipalComponent
import io.github.aplcornell.viaduct.security.LabelComponent
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.DelegationProjection
import io.github.aplcornell.viaduct.syntax.FunctionNameNode
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.LabelNode
import io.github.aplcornell.viaduct.syntax.LabelVariableNode
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.ObjectVariableNode
import io.github.aplcornell.viaduct.syntax.ParameterDirection
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node() {
    abstract override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.TopLevelDeclarationNode
}

/**
 * Declaration of a participant and their authority.
 *
 * @param name Host name.
 * @param authority Label specifying the trust placed in this host.
 */
class HostDeclarationNode(
    val name: HostNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {

    override val children: Iterable<Nothing>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.HostDeclarationNode =
        io.github.aplcornell.viaduct.syntax.surface.HostDeclarationNode(
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

    override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.ParameterNode =
        io.github.aplcornell.viaduct.syntax.surface.ParameterNode(
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
    val labelParameters: Arguments<LabelVariableNode>,
    val parameters: Arguments<ParameterNode>,
    val labelConstraints: Arguments<IFCDelegationDeclarationNode>,
    val pcLabel: LabelNode,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val children: Iterable<Node>
        get() = (parameters.toPersistentList() as PersistentList<Node>).add(body)

    override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.FunctionDeclarationNode =
        io.github.aplcornell.viaduct.syntax.surface.FunctionDeclarationNode(
            name,
            labelParameters,
            Arguments(
                parameters.map { it.toSurfaceNode(metadata) },
                parameters.sourceLocation
            ),
            Arguments(
                labelConstraints.map {
                    it.toSurfaceNode()
                },
                labelConstraints.sourceLocation
            ),
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

abstract class DelegationDeclarationNode(
    open val from: LabelNode,
    open val to: LabelNode,
    open val delegationProjection: DelegationProjection,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    abstract fun congruences(): List<FreeDistributiveLattice.LessThanOrEqualTo<PrincipalComponent>>
    abstract override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.DelegationDeclarationNode
}

/**
 * Declaration of a delegation.
 *
 */
class AuthorityDelegationDeclarationNode(
    override val from: LabelNode,
    override val to: LabelNode,
    override val delegationProjection: DelegationProjection,
    override val sourceLocation: SourceLocation
) : DelegationDeclarationNode(from, to, delegationProjection, sourceLocation) {

    override fun congruences(): List<FreeDistributiveLattice.LessThanOrEqualTo<PrincipalComponent>> {
        var fromConfidentiality: LabelComponent =
            from.value.interpret().confidentialityComponent
        var fromIntegrity: LabelComponent = from.value.interpret().integrityComponent
        var toConfidentiality: LabelComponent =
            to.value.interpret().confidentialityComponent
        var toIntegrity: LabelComponent = to.value.interpret().integrityComponent

        fromConfidentiality = toConfidentiality.also { toConfidentiality = fromConfidentiality }
        fromIntegrity = toIntegrity.also { toIntegrity = fromIntegrity }

        return when (delegationProjection) {
            DelegationProjection.CONFIDENTIALITY ->
                listOf(FreeDistributiveLattice.LessThanOrEqualTo(fromConfidentiality, toConfidentiality))

            DelegationProjection.INTEGRITY ->
                listOf(FreeDistributiveLattice.LessThanOrEqualTo(fromIntegrity, toIntegrity))

            DelegationProjection.BOTH ->
                listOf(
                    FreeDistributiveLattice.LessThanOrEqualTo(fromConfidentiality, toConfidentiality),
                    FreeDistributiveLattice.LessThanOrEqualTo(fromIntegrity, toIntegrity)
                )
        }
    }

    override val children: Iterable<BlockNode>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.AuthorityDelegationDeclarationNode =
        io.github.aplcornell.viaduct.syntax.surface.AuthorityDelegationDeclarationNode(
            from,
            to,
            delegationProjection,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): AuthorityDelegationDeclarationNode =
        AuthorityDelegationDeclarationNode(from, to, delegationProjection, sourceLocation)
}

/**
 * Declaration of a delegation.
 *
 */
class IFCDelegationDeclarationNode(
    override val from: LabelNode,
    override val to: LabelNode,
    override val delegationProjection: DelegationProjection,
    override val sourceLocation: SourceLocation
) : DelegationDeclarationNode(from, to, delegationProjection, sourceLocation) {

    override fun congruences(): List<FreeDistributiveLattice.LessThanOrEqualTo<PrincipalComponent>> {
        var fromConfidentiality: LabelComponent =
            from.value.interpret().confidentialityComponent
        var fromIntegrity: LabelComponent = from.value.interpret().integrityComponent
        var toConfidentiality: LabelComponent =
            to.value.interpret().confidentialityComponent
        var toIntegrity: LabelComponent = to.value.interpret().integrityComponent

        fromConfidentiality = toConfidentiality.also { toConfidentiality = fromConfidentiality }

        return when (delegationProjection) {
            DelegationProjection.CONFIDENTIALITY ->
                listOf(FreeDistributiveLattice.LessThanOrEqualTo(fromConfidentiality, toConfidentiality))

            DelegationProjection.INTEGRITY ->
                listOf(FreeDistributiveLattice.LessThanOrEqualTo(fromIntegrity, toIntegrity))

            DelegationProjection.BOTH ->
                listOf(
                    FreeDistributiveLattice.LessThanOrEqualTo(fromConfidentiality, toConfidentiality),
                    FreeDistributiveLattice.LessThanOrEqualTo(fromIntegrity, toIntegrity)
                )
        }
    }

    override val children: Iterable<BlockNode>
        get() = listOf()

    override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.IFCDelegationDeclarationNode =
        io.github.aplcornell.viaduct.syntax.surface.IFCDelegationDeclarationNode(
            from,
            to,
            delegationProjection,
            sourceLocation,
            comment = metadataAsComment(metadata)
        )

    override fun copy(children: List<Node>): IFCDelegationDeclarationNode =
        IFCDelegationDeclarationNode(from, to, delegationProjection, sourceLocation)
}
