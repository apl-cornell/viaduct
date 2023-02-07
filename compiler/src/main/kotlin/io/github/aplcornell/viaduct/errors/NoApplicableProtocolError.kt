package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.syntax.HasSourceLocation
import io.github.aplcornell.viaduct.syntax.Protocol

/** Thrown when there is no [Protocol] that can be assigned to [node]. */
class NoApplicableProtocolError(private val node: HasSourceLocation) : CompilationError() {
    override val category: String
        get() = "No Applicable Protocol"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        // TODO: give more information to ease debugging. For example,
        //   - Is the problem syntactic restrictions or authority?
        //   - Show the highest authority that can be achieved.
        //   - Smartly recommend adding hosts, increasing host capabilities, or changing labels in the code.
        get() =
            Document("There is no protocol that can implement this code:")
                .withSource(node.sourceLocation)
}
