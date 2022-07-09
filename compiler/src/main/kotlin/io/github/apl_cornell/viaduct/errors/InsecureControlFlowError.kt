package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.div
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

/**
 * Thrown when the control flow influences data in a way that violates security.
 *
 * @param node AST node influenced by control flow.
 * @param nodeLabel Security label of the node.
 * @param pc Security label assigned to control flow.
 */
class InsecureControlFlowError(
    private val node: HasSourceLocation,
    private val nodeLabel: Label,
    private val pc: Label
) : InformationFlowError() {
    override val category: String
        get() = "Insecure Control Flow"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() {
            if (!pc.confidentiality().flowsTo(nodeLabel.confidentiality())) {
                // Confidentiality is the problem
                // TODO: reword message (see the output of insecure-control-flow-confidentiality.via)
                return Document("Execution of this term might leak information encoded in the control flow:")
                    .withSource(node.sourceLocation) /
                    Document("Confidentiality label on control flow is:")
                        .withData(pc.confidentiality()) /
                    Document("But the term only guarantees:")
                        .withData(nodeLabel.confidentiality())
            } else {
                // Integrity is the problem
                // TODO: add an error test case that covers this branch.
                assert(!pc.integrity().flowsTo(nodeLabel.integrity()))
                return Document("The control flow does not have enough integrity for this term:")
                    .withSource(node.sourceLocation) /
                    Document("Integrity label on control flow is:")
                        .withData(pc.integrity()) /
                    Document("But it needs to be at least:")
                        .withData(nodeLabel.integrity())
            }
        }
}
