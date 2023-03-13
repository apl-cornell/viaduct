package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.syntax.HasSourceLocation

/** Thrown when a protocol is applied to bad arguments. */
class MalformedProtocolAnnotationError(
    private val annotation: HasSourceLocation,
    private val reason: String,
) : CompilationError() {
    override val category: String
        get() = "Malformed Protocol Annotation"

    override val source: String
        get() = annotation.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("${reason.replaceFirstChar { it.uppercaseChar() }}:")
                .withSource(annotation.sourceLocation)
}
