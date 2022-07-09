package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.syntax.ArgumentLabel
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.SourceLocation

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
