package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.confidentiality
import io.github.aplcornell.viaduct.syntax.intermediate.EndorsementNode

/**
 * Thrown when a [EndorsementNode] modifies integrity.
 *
 * @param node Endorsement statement that modifies confidentiality.
 * @param from Label of the expression being endorsed.
 * @param to Resulting label of the expression.
 */
class ConfidentialityChangingEndorsementError(private val node: EndorsementNode, from: Label, to: Label) :
    InformationFlowError() {
    private val fromConfidentiality: Label = from.confidentiality()
    private val toConfidentiality: Label = to.confidentiality()

    override val category: String
        get() = "Endorsement Changes Confidentiality"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This endorsement modifies confidentiality:")
                .withSource(node.sourceLocation) /
                Document("Original confidentiality of the expression:")
                    .withData(fromConfidentiality) /
                Document("Output confidentiality:")
                    .withData(toConfidentiality)
}
