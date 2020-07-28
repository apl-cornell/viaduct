package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.passes.elaborated
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

    override val children: Iterable<TopLevelDeclarationNode>
        get() = declarations

    override fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode =
        edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode(
            declarations.map { it.toSurfaceNode() },
            sourceLocation
        )

    override fun toString(): String =
        "Program (" + sourceLocation.sourcePath + ")"
}
