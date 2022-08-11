package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.concatenated
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * The circuit representation of a program.
 */
class ProgramNode(
    val declarations: PersistentList<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    constructor(declarations: List<TopLevelDeclarationNode>, sourceLocation: SourceLocation) :
        this(declarations.toPersistentList(), sourceLocation)

    val hostDeclarations: Iterable<HostDeclarationNode> =
        declarations.filterIsInstance<HostDeclarationNode>()

    val hosts: Set<Host> = hostDeclarations.map { it.name.value }.toSet()

    val circuits: Iterable<CircuitDeclarationNode> =
        declarations.filterIsInstance<CircuitDeclarationNode>()

    override fun toDocument(): Document =
        declarations.concatenated(Document.forcedLineBreak + Document.forcedLineBreak)
}
