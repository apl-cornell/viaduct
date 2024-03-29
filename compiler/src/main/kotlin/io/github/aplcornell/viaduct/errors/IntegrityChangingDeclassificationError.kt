package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.integrity
import io.github.aplcornell.viaduct.syntax.intermediate.DeclassificationNode

/**
 * Thrown when a [DeclassificationNode] modifies integrity.
 *
 * @param node Declassify statement that modifies integrity.
 * @param from Label of the expression being declassified.
 * @param to Resulting label of the expression.
 */
class IntegrityChangingDeclassificationError(private val node: DeclassificationNode, from: Label, to: Label) :
    InformationFlowError() {
    private val fromIntegrity: Label = from.integrity()
    private val toIntegrity: Label = to.integrity()

    override val category: String
        get() = "Declassify Changes Integrity"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This declassification modifies integrity:")
                .withSource(node.sourceLocation) /
                Document("Original integrity of the expression:")
                    .withData(fromIntegrity) /
                Document("Output integrity:")
                    .withData(toIntegrity)
}
