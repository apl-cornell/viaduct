package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.attributes.Attribute
import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.passes.elaborated
import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.SourceLocation
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
    val hostDeclarations: Iterable<HostDeclarationNode>
        get() = declarations.filterIsInstance<HostDeclarationNode>()

    // TODO: Should be provided by HostTrustConfiguration
    val hosts: Set<Host>
        get() = hostDeclarations.map { it.name.value }.toSet()

    // TODO: Should be moved to analysis.Declarations
    val functions: Iterable<FunctionDeclarationNode>
        get() = declarations.filterIsInstance<FunctionDeclarationNode>()

    val functionMap: Map<FunctionName, FunctionDeclarationNode>
        get() = functions.associateBy { function -> function.name.value }

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

    override fun toSurfaceNode(metadata: Metadata): io.github.aplcornell.viaduct.syntax.surface.ProgramNode =
        io.github.aplcornell.viaduct.syntax.surface.ProgramNode(
            declarations.map { it.toSurfaceNode(metadata) },
            sourceLocation
        )

    override fun copy(children: List<Node>): ProgramNode =
        ProgramNode(children.map { it as TopLevelDeclarationNode }, sourceLocation)

    override fun toString(): String =
        "Program (" + sourceLocation.sourcePath + ")"
}
