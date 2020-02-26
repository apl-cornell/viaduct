package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

/**
 * Thrown when the control flow influences data in a way that violates security.
 *
 * @param node AST node influenced by control flow.
 * @param nodeLabel Security label of [node].
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
                return Document("Execution of this term might leak information encoded in the control flow:")
                    .withSource(node.sourceLocation) +
                    Document("Confidentiality label on control flow is:")
                        .withData(pc.confidentiality()) +
                    Document("But the term only guarantees:")
                        .withData(nodeLabel.confidentiality())
            } else {
                // Integrity is the problem
                assert(!pc.integrity().flowsTo(nodeLabel.integrity()))
                return Document("The control flow does not have enough integrity for this term:")
                    .withSource(node.sourceLocation) +
                    Document("Integrity label on control flow is:")
                        .withData(pc.integrity()) +
                    Document("But it needs to be at least:")
                        .withData(nodeLabel.integrity())
            }
        }
}
