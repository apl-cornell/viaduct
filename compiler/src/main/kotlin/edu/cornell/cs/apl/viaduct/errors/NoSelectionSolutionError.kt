package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document

/**
 * Thrown when attempting to mux a statement that cannot be muxed.
 *
 * @param statement statement
 */
class NoSelectionSolutionError : CompilationError() {
    override val category: String
        get() = "No Selection Solution"

    override val source: String
        get() = ""

    override val description: Document
        get() = Document("Could not find a protocol selection for this program.\n")
}
