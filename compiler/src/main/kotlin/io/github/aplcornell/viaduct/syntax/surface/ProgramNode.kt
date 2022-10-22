package io.github.apl_cornell.viaduct.syntax.surface

import io.github.apl_cornell.viaduct.parsing.parse
import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.concatenated
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * Represents a surface level program which is a sequence of top level declarations.
 *
 * Instances are usually created by [parse].
 */
class ProgramNode
private constructor(
    val declarations: PersistentList<TopLevelDeclarationNode>,
    override val sourceLocation: SourceLocation
) : Node(), List<TopLevelDeclarationNode> by declarations {
    constructor(declarations: List<TopLevelDeclarationNode>, sourceLocation: SourceLocation) :
        this(declarations.toPersistentList(), sourceLocation)

    override val comment: String?
        get() = null

    override fun toDocumentWithoutComment(): Document =
        declarations.concatenated(Document.forcedLineBreak + Document.forcedLineBreak)

    override fun toString(): String =
        "Program (" + sourceLocation.sourcePath + ")"
}
