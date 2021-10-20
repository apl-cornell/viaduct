package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.attributes.Attribute
import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.commented
import edu.cornell.cs.apl.prettyprinting.concatenated
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * The intermediate level representation of a program.
 *
 * Instances are created by [elaborated].
 */
class ProgramNode
private constructor(
    val declarations: PersistentList<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    constructor(declarations: List<TopLevelDeclarationNode>, sourceLocation: SourceLocation) :
        this(declarations.toPersistentList(), sourceLocation)

    // TODO: Should be moved to analysis.Declarations
    val hostDeclarations: Iterable<HostDeclarationNode> =
        declarations.filterIsInstance<HostDeclarationNode>()

    // TODO: Should be provided by HostTrustConfiguration
    val hosts: Set<Host> = hostDeclarations.map { it.name.value }.toSet()

    // TODO: Should be moved to analysis.Declarations
    val functions: Iterable<FunctionDeclarationNode> =
        declarations.filterIsInstance<FunctionDeclarationNode>()

    // TODO: Should be moved to analysis.NameAnalysis
    val functionMap: Map<FunctionName, FunctionDeclarationNode> =
        functions.associateBy { function -> function.name.value }

    /** A lazily constructed [Tree] instance for the program. */
    val tree: Tree<Node, ProgramNode> by lazy { Tree(this) }

    private val functionCache: Attribute<(ProgramNode) -> Any?, Any?> = attribute {
        this.invoke(this@ProgramNode)
    }

    /**
     * Applies [function] to this program and returns the results.
     * The result is cached, so future calls with the same function do not evaluate [function].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> cached(function: (ProgramNode) -> T): T =
        functionCache(function) as T

    override val children: Iterable<TopLevelDeclarationNode>
        get() = declarations

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode(
            declarations.map { it.toSurfaceNode() },
            sourceLocation
        )

    override fun copy(children: List<Node>): ProgramNode =
        ProgramNode(children.map { it as TopLevelDeclarationNode }, sourceLocation)

    override fun toString(): String =
        "Program (" + sourceLocation.sourcePath + ")"

    override fun printMetadata(metadata: Map<Node, PrettyPrintable>): Document =
        (metadata[this]?.let { it.asDocument.commented() + Document.forcedLineBreak } ?: Document("")) +
            declarations
                .map { it.printMetadata(metadata) }
                .concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
}
