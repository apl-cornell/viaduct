package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode

/**
 * Thrown when an [EndorsementNode] modifies confidentiality.
 *
 * @param node Endorse statement that modifies confidentiality.
 * @param from Label of the expression being declassified.
 * @param to Resulting label of the expression.
 */
class ConfidentialityChangingEndorsementError(private val node: EndorsementNode, from: Label, to: Label) :
    InformationFlowError() {
    private val fromConfidentiality: Label = from.confidentiality()
    private val toConfidentiality: Label = to.confidentiality()

    override val category: String
        get() = "Endorse Changes Confidentiality"

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
