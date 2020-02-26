package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.Variable

/**
 * A node in the intermediate language abstract syntax tree.
 *
 * The intermediate language is similar to the surface language, but with the following changes:
 *
 * - For and while loops are elaborated into loop-until-break statements.
 * - Expressions are in A-normal form. Briefly, this means all intermediate results are stored
 *   in temporary variables.
 * - Every loop and break statement has a [JumpLabel].
 * - All [Variable]s within a process have unique names.
 */
abstract class Node : HasSourceLocation, PrettyPrintable {
    /**
     * Returns a representation of this node in the surface syntax.
     *
     * This is useful, for example, for [pretty printing][PrettyPrintable].
     */
    abstract fun toSurfaceNode(): edu.cornell.cs.apl.viaduct.syntax.surface.Node

    final override val asDocument: Document
        get() = toSurfaceNode().asDocument
}
