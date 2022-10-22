package io.github.apl_cornell.viaduct.syntax.surface

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.prettyprinting.commented
import io.github.apl_cornell.viaduct.prettyprinting.div
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

/**
 * A node in the abstract syntax tree of a surface level program.
 *
 * The topmost level node is [ProgramNode].
 */
abstract class Node : HasSourceLocation, PrettyPrintable {
    abstract val comment: String?

    final override fun toDocument(): Document = comment?.let { Document(it).commented() / toDocumentWithoutComment() }
        ?: toDocumentWithoutComment()

    /** The pretty text representation of this node ignoring the [comment] property. */
    protected abstract fun toDocumentWithoutComment(): Document
}
