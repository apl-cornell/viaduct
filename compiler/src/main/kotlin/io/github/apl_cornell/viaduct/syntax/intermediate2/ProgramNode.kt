package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.passes.elaborated
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList

/**
 * The intermediate level representation of a program.
 *
 * Instances are created by [elaborated].
 */
class ProgramNode
private constructor(
    val declarations: PersistentList<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations
