package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.toPersistentList

/** A program is a sequence of top level declarations. */
class ProgramNode(
    declarations: List<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    // Make an immutable copy
    val declarations: List<TopLevelDeclarationNode> = declarations.toPersistentList()
}
