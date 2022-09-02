package io.github.apl_cornell.viaduct.errors

/**
 * Thrown when the control flow influences data in a way that violates security.
 *
 * @param node AST node influenced by control flow.
 * @param nodeLabel Security label of the node.
 * @param pc Security label assigned to control flow.
 */
/*
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
            // TODO: use flowsTo rather than actsFor
            if (!nodeLabel.confidentiality(FreeDistributiveLattice.bounds())
                    .actsFor(pc.confidentiality(FreeDistributiveLattice.bounds()))
            ) {
                // Confidentiality is the problem
                // TODO: reword message (see the output of insecure-control-flow-confidentiality.via)
                return Document("Execution of this term might leak information encoded in the control flow:")
                    .withSource(node.sourceLocation) /
                    Document("Confidentiality label on control flow is:")
                        .withData(pc.confidentiality(FreeDistributiveLattice.bounds())) /
                    Document("But the term only guarantees:")
                        .withData(nodeLabel.confidentiality(FreeDistributiveLattice.bounds()))
            } else {
                // Integrity is the problem
                // TODO: add an error test case that covers this branch.
                // TODO: use flowsTo rather than actsFor
                assert(
                    !pc.integrity(FreeDistributiveLattice.bounds())
                        .actsFor(nodeLabel.integrity(FreeDistributiveLattice.bounds()))
                )
                return Document("The control flow does not have enough integrity for this term:")
                    .withSource(node.sourceLocation) /
                    Document("Integrity label on control flow is:")
                        .withData(pc.integrity(FreeDistributiveLattice.bounds())) /
                    Document("But it needs to be at least:")
                        .withData(nodeLabel.integrity(FreeDistributiveLattice.bounds()))
            }
        }
}
*/
