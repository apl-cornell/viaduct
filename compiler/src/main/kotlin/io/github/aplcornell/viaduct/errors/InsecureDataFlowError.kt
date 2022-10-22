package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.div
import io.github.apl_cornell.viaduct.security.Component
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.security.confidentiality
import io.github.apl_cornell.viaduct.security.flowsTo
import io.github.apl_cornell.viaduct.security.integrity
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

/**
 * Thrown when a term's output flows to a location that would violate information flow security.
 */
class InsecureDataFlowError(
    private val node: HasSourceLocation,
    private val nodeLabel: Label,
    private val to: Label,
    private val context: FreeDistributiveLatticeCongruence<Component<Principal>>
) : InformationFlowError() {
    override val category: String
        get() = "Insecure Data Flow"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() {
            if (!flowsTo(nodeLabel.integrity(), to.integrity(), context)) {
                // Confidentiality is the problem
                return Document("This term is flowing to a place that does not have enough confidentiality:")
                    .withSource(node.sourceLocation) /
                    Document("The term's confidentiality label is:")
                        .withData(nodeLabel.confidentiality()) /
                    Document("But it is going to a place that only guarantees:")
                        .withData(to.confidentiality())
            } else {
                assert(!flowsTo(nodeLabel.confidentiality(), to.confidentiality(), context))
                return Document("This term does not have enough integrity:")
                    .withSource(node.sourceLocation) /
                    Document("Its integrity label is:")
                        .withData(nodeLabel.integrity()) /
                    Document("But it needs to be at least:")
                        .withData(to.integrity())
            }
        }
}
