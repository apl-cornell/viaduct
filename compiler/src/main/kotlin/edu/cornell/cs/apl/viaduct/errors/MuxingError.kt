package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode

/**
 * Thrown when attempting to mux a statement that cannot be muxed.
 *
 * @param statement statement
 */
class MuxingError(
    private val statement: StatementNode
) : CompilationError() {
    override val category: String
        get() = "Unknown Datatype"

    override val source: String
        get() = statement.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This statement cannot be muxed:")
                .withSource(statement.sourceLocation)
}
