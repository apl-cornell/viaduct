package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.ArgumentLabel
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

/** Thrown when a function is given an extra argument. */
class UnexpectedArgumentError(name: Located<ArgumentLabel>) : CompilationError() {
    private val name: ArgumentLabel = name.value
    private val location: SourceLocation = name.sourceLocation

    override val category: String
        get() = "Extra Argument"

    override val source: String
        get() = location.sourcePath

    override val description: Document
        get() =
            (Document("I do not expect an argument named") * name * "here:")
                .withSource(location)
}
