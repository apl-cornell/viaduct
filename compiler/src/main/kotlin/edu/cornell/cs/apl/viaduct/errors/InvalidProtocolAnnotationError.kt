package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node

/** Thrown when the protocol [node] is annotated with cannot implement [node]. */
class InvalidProtocolAnnotationError(private val node: Node) : CompilationError() {
    override val category: String
        get() = "Invalid Protocol Annotation"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("Invalid protocol annotation:")
                .withSource(node.sourceLocation)
}
