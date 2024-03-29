package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.syntax.intermediate.Node

/** Thrown when a required protocol annotation in [node] is not found. */
class NoProtocolAnnotationError(private val node: Node) : CompilationError() {
    override val category: String
        get() = "No Protocol Annotation"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("Protocol annotation required here:")
                .withSource(node.sourceLocation)
}
