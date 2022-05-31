package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectTypeNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ParameterDirection
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell

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
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : TopLevelDeclarationNode() {
    override fun toDocumentWithoutComment(): Document = keyword("host") * name * ":" * listOf(authority).braced()
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
    val pcLabel: LabelNode?,
    val parameters: Arguments<ParameterNode>,
    val body: BlockNode,
    override val sourceLocation: SourceLocation,
    override val comment: String? = null
) : TopLevelDeclarationNode() {
    override fun toDocumentWithoutComment(): Document =
        keyword("fun") * name +
            (pcLabel?.let { listOf(it).braced() } ?: Document("")) +
            parameters.tupled() * body
}


/* Delegation syntax */

/**
 * Declaration of a delegations.
 * @param node1 The label that acts for the other label.
 * @param node2 The other label.
 * @param is_mutual True iff the delegation is on both directions.
 */
class DelegationDeclarationNode(
    val node1: LabelNode,
    val node2: LabelNode,
    override val sourceLocation: SourceLocation,
    override val comment: String?
) : TopLevelDeclarationNode() {
    override fun toDocumentWithoutComment(): Document =
        keyword("delegation:") * listOf(node1).braced() *
            "=>" * listOf(node2).braced()
}
