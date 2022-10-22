package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.aplcornell.viaduct.passes.PrincipalComponent
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.confidentiality
import io.github.aplcornell.viaduct.security.flowsTo
import io.github.aplcornell.viaduct.security.integrity
import io.github.aplcornell.viaduct.syntax.HasSourceLocation

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
    private val pc: Label,
    private val context: FreeDistributiveLatticeCongruence<PrincipalComponent>
) : InformationFlowError() {
    override val category: String
        get() = "Insecure Control Flow"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() {
            // TODO: use flowsTo rather than actsFor
            if (!flowsTo(
                    pc.confidentiality(),
                    nodeLabel.confidentiality(),
                    context
                )
            ) {
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
                // TODO: use flowsTo rather than actsFor
                assert(
                    !flowsTo(
                        pc.integrity(),
                        nodeLabel.integrity(),
                        context
                    )
                )
                return Document("The control flow does not have enough integrity for this term:")
                    .withSource(node.sourceLocation) /
                    Document("Integrity label on control flow is:")
                        .withData(pc.integrity()) /
                    Document("But it needs to be at least:")
                        .withData(nodeLabel.integrity())
            }
        }
}
