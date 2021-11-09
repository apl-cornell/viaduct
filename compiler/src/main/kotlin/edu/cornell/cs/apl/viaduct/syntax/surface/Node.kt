package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.commented
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

/**
 * A node in the abstract syntax tree of a surface level program.
 *
 * The topmost level node is [ProgramNode].
 */
abstract class Node : HasSourceLocation, PrettyPrintable {
    abstract val comment: String?

    final override fun asDocument(): Document = comment?.let { Document(it).commented() / asDocumentWithoutComment() }
        ?: asDocumentWithoutComment()

    /** The pretty text representation of this node ignoring the [comment] property. */
    protected abstract fun asDocumentWithoutComment(): Document
}
