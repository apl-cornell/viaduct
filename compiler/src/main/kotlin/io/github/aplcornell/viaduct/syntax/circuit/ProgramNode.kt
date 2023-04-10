package io.github.aplcornell.viaduct.syntax.circuit

import io.github.aplcornell.viaduct.attributes.Attribute
import io.github.aplcornell.viaduct.attributes.Tree
import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.concatenated
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * The circuit representation of a program.
 */
class ProgramNode(
    val declarations: PersistentList<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation,
) : Node(), List<TopLevelDeclarationNode> by declarations {
    constructor(declarations: List<TopLevelDeclarationNode>, sourceLocation: SourceLocation) :
        this(declarations.toPersistentList(), sourceLocation)

    override val children: Iterable<Node>
        get() = declarations

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

    val hostDeclarations: Iterable<HostDeclarationNode> =
        declarations.filterIsInstance<HostDeclarationNode>()

    val hosts: Set<Host> = hostDeclarations.map { it.name.value }.toSet()

    val circuits: Iterable<CircuitDeclarationNode> =
        declarations.filterIsInstance<CircuitDeclarationNode>()

    val functions: Iterable<FunctionDeclarationNode> =
        declarations.filterIsInstance<FunctionDeclarationNode>()

    override fun toDocument(): Document =
        declarations.concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
}
