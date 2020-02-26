package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

/**
 * Thrown when a term's output flows to a location that would violate information flow security.
 */
class InsecureDataFlowError(
    private val node: HasSourceLocation,
    private val nodeLabel: Label,
    private val to: Label
) : InformationFlowError() {
    override val category: String
        get() = "Insecure Data Flow"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() {
            if (!nodeLabel.confidentiality().flowsTo(to.confidentiality())) {
                // Confidentiality is the problem
                return Document("This term is flowing to a place that does not have enough confidentiality:")
                    .withSource(node.sourceLocation) +
                    Document("The term's confidentiality label is:")
                        .withData(nodeLabel.confidentiality()) +
                    Document("But it is going to a place that only guarantees:")
                        .withData(to.confidentiality())
            } else {
                // Integrity is the problem
                assert(!nodeLabel.integrity().flowsTo(to.integrity()))
                return Document("This term does not have enough integrity:")
                    .withSource(node.sourceLocation) +
                    Document("Its integrity label is:")
                        .withData(nodeLabel.integrity()) +
                    Document("But it needs to be at least:")
                        .withData(to.integrity())
            }
        }
}
