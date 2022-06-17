package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.div
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

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
            // TODO: use flowsTo rather than actsFor
            if (!to.confidentiality(FreeDistributiveLattice.bounds())
                    .actsFor(nodeLabel.confidentiality(FreeDistributiveLattice.bounds()))
            ) {
                // Confidentiality is the problem
                return Document("This term is flowing to a place that does not have enough confidentiality:")
                    .withSource(node.sourceLocation) /
                    Document("The term's confidentiality label is:")
                        .withData(nodeLabel.confidentiality(FreeDistributiveLattice.bounds())) /
                    Document("But it is going to a place that only guarantees:")
                        .withData(to.confidentiality(FreeDistributiveLattice.bounds()))
            } else {
                // Integrity is the problem
                // TODO: use flowsTo rather than actsFor
                assert(
                    !nodeLabel.integrity(FreeDistributiveLattice.bounds())
                        .actsFor(to.integrity(FreeDistributiveLattice.bounds()))
                )
                return Document("This term does not have enough integrity:")
                    .withSource(node.sourceLocation) /
                    Document("Its integrity label is:")
                        .withData(nodeLabel.integrity(FreeDistributiveLattice.bounds())) /
                    Document("But it needs to be at least:")
                        .withData(to.integrity(FreeDistributiveLattice.bounds()))
            }
        }
}
