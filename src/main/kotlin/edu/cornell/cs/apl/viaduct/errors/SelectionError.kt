package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times

/**
 * Thrown when an error is found during protocol selection.
 *
 * @param info Description of the error.
 */
class SelectionError(
    val info: String
) : Error(), PrettyPrintable {
    override val asDocument: Document
        get() = Document(info)
}
