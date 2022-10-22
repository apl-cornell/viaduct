package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.ArgumentLabel
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.SourceLocation

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
