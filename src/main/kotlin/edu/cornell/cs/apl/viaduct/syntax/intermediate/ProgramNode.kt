package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.toPersistentList

/**
 * The intermediate level representation of a program.
 *
 * Instances are created by [elaborated].
 */
class ProgramNode(
    declarations: List<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    // Make an immutable copy
    val declarations: List<TopLevelDeclarationNode> = declarations.toPersistentList()

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode(
            declarations.map { it.toSurfaceNode() },
            sourceLocation
        )
}
