package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.surface.Node

class InvalidConstructorCallError(
    private val node: Node,
    private val constructorNeeded: Boolean = false
) : CompilationError() {
    override val category: String
        get() =
            if (constructorNeeded) {
                "Invalid Constructor Call"
            } else {
                "Needed Constructor Call"
            }

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            if (constructorNeeded) {
                Document("Constructor call needed here:")
                    .withSource(node.sourceLocation)
            } else {
                Document("Cannot invoke constructor here:")
                    .withSource(node.sourceLocation)
            }
}
