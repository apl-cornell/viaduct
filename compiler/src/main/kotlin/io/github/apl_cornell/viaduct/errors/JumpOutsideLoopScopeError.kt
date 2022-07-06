package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

/**
 * Thrown when a `break`, `continue`, or a similar statement occurs outside the scope of a loop.
 */
class JumpOutsideLoopScopeError(val node: HasSourceLocation) : CompilationError() {
    override val category: String
        get() = "Control Error"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This statement is only valid inside a loop:")
                .withSource(node.sourceLocation)
}
