package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.commented
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.FunctionNameNode
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ParameterDirection
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.surface.keyword
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node() {
    abstract override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.TopLevelDeclarationNode
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

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.HostDeclarationNode(
            name,
            authority,
            sourceLocation
        )

    override fun copy(children: List<Node>): HostDeclarationNode =
        HostDeclarationNode(name, authority, sourceLocation)
}

/**
 * A process declaration associating a protocol with the code that process should run.
 *
 * @param protocol Name of the process.
 * @param body Code that will be executed by this process.
 */
class ProcessDeclarationNode(
    val protocol: ProtocolNode,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode() {
    override val children: Iterable<BlockNode>
        get() = listOf(body)

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ProcessDeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ProcessDeclarationNode(
            protocol,
            body.toSurfaceNode(),
            sourceLocation
        )

    override fun copy(children: List<Node>): ProcessDeclarationNode =
        ProcessDeclarationNode(protocol, children[0] as BlockNode, sourceLocation)

    override fun printMetadata(metadata: Map<Node, PrettyPrintable>): Document =
        (metadata[this]?.let { it.asDocument.commented() + Document.forcedLineBreak } ?: Document("")) +
        keyword("process") * protocol * body.printMetadata(metadata)
}

/**
 * A parameter to a function declaration.
 */
class ParameterNode(
    override val name: ObjectVariableNode,
    val parameterDirection: ParameterDirection,
    override val className: ClassNameNode,
    override val typeArguments: Arguments<ValueTypeNode>,
    // TODO: allow leaving out some of the labels (right now it's all or nothing)
    override val labelArguments: Arguments<LabelNode>?,
    val protocol: ProtocolNode?,
    override val sourceLocation: SourceLocation
) : Node(), ObjectDeclaration {
    override val declarationAsNode: Node
        get() = this

    override val children: Iterable<BlockNode>
        get() = listOf()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.Node =
        edu.cornell.cs.apl.viaduct.syntax.surface.ParameterNode(
            name,
            parameterDirection,
            className,
            typeArguments,
            labelArguments,
            protocol,
            sourceLocation
        )

    override fun copy(children: List<Node>): Node =
        ParameterNode(name, parameterDirection, className, typeArguments, labelArguments, protocol, sourceLocation)

    val isOutParameter: Boolean
        get() = parameterDirection == ParameterDirection.PARAM_OUT

    val isInParameter: Boolean
        get() = parameterDirection == ParameterDirection.PARAM_IN
}

/**
 * A declaration of a function that can be called by a process.
 *
 * @param parameters A list of formal parameters.
 * @param body The function body.
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

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.TopLevelDeclarationNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.FunctionDeclarationNode(
            name,
            pcLabel,
            Arguments(
                parameters.map { param ->
                    param.toSurfaceNode() as edu.cornell.cs.apl.viaduct.syntax.surface.ParameterNode
                },
                parameters.sourceLocation
            ),
            body.toSurfaceNode(),
            sourceLocation
        )

    override fun copy(children: List<Node>): Node {
        val parameters = Arguments(children.dropLast(1).map { it as ParameterNode }, parameters.sourceLocation)
        return FunctionDeclarationNode(name, pcLabel, parameters, children.last() as BlockNode, sourceLocation)
    }

    override fun printMetadata(metadata: Map<Node, PrettyPrintable>): Document =
        (metadata[this]?.let { it.asDocument.commented() + Document.forcedLineBreak } ?: Document("")) +
        keyword("fun") * name +
            (pcLabel?.let { listOf(it).braced() } ?: Document("")) +
            parameters.tupled() * body.printMetadata(metadata)

    fun getParameter(name: ObjectVariable): ParameterNode? =
        parameters.firstOrNull { param -> param.name.value == name }

    fun getParameterAtIndex(i: Int): ParameterNode? =
        if (i >= 0 && i < parameters.size) parameters[i] else null
}
