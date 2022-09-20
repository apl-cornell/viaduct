package io.github.apl_cornell.viaduct.syntax.surface

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.braced
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.prettyprinting.tupled
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.DelegationKind
import io.github.apl_cornell.viaduct.syntax.DelegationProjection
import io.github.apl_cornell.viaduct.syntax.FunctionNameNode
import io.github.apl_cornell.viaduct.syntax.HostNode
import io.github.apl_cornell.viaduct.syntax.LabelNode
import io.github.apl_cornell.viaduct.syntax.LabelVariableNode
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.ParameterDirection
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.datatypes.MutableCell

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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : TopLevelDeclarationNode() {
    override fun toDocumentWithoutComment(): Document = keyword("host") * name
}

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    val name: ObjectVariableNode,
    val parameterDirection: ParameterDirection,
    val objectType: ObjectTypeNode,
    val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : Node() {
    init {
        assert(objectType.className.value != MutableCell)
    }

    override fun toDocumentWithoutComment(): Document =
        name + Document(":") + parameterDirection * objectType.toDocument(protocol)
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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : TopLevelDeclarationNode() {
    override fun toDocumentWithoutComment(): Document =
        keyword("fun") *
            (labelParameters?.braced() ?: Document("")) *
            name +
            parameters.tupled() * (labelConstraints?.braced() ?: Document("")) *
            (pcLabel?.let { Document(":") + listOf(it).braced() } ?: Document("")) * body
}

/* Delegation syntax */

/**
 * Declaration of a delegations.
 * @param from The label that acts for the other label.
 * @param to The other label.
 * @param delegationKind is either IFC or AUTHORITY depending on what kind it is.
 */
class DelegationDeclarationNode(
    val from: LabelNode,
    val to: LabelNode,
    val delegationKind: DelegationKind,
    val delegationProjection: DelegationProjection,
    override val sourceLocation: SourceLocation,
    override val comment: String?
) : TopLevelDeclarationNode() {
    override fun toDocumentWithoutComment(): Document =
        keyword("delegation:") * listOf(from).braced() *
            (
                when (delegationKind) {
                    DelegationKind.AUTHORITY -> "trusts"
                    DelegationKind.IFC -> ":>"
                }
                ) * listOf(to).braced() * "for" * delegationProjection
}
