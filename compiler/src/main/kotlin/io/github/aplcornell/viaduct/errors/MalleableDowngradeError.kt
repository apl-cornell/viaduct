package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.syntax.intermediate.DowngradeNode

/**
 * Thrown when a [DowngradeNode] node violate the non-malleable information flow control
 * restriction.
 */
class MalleableDowngradeError(private val node: DowngradeNode) : InformationFlowError() {
    override val category: String
        get() = "Malleable Downgrade"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            // TODO: give more information to help diagnose the error?
            Document("This downgrade violates the non-malleability condition:")
                .withSource(node.sourceLocation)
}
