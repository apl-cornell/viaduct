package io.github.aplcornell.viaduct.syntax.surface

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.prettyprinting.commented
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.syntax.HasSourceLocation

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
