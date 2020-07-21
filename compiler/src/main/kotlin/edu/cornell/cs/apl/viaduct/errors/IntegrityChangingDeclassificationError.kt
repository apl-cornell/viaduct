package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode

/**
 * Thrown when a [DeclassificationNode] modifies confidentiality.
 *
 * @param node Declassify statement that modifies confidentiality.
 * @param from Label of the expression being declassified.
 */
class IntegrityChangingDeclassificationError(private val node: DeclassificationNode, from: Label) :
    InformationFlowError() {
    private val fromIntegrity: Label = from.integrity()

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
                    .withData(node.toLabel.value.integrity())
}
