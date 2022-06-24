package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.div
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

// TODO: I don't think there is an example that throws this error...

/**
 * Thrown when the inferred label of a node does not match its annotated label.
 *
 * @param actualLabel Inferred label for the node.
 * @param expectedLabel Annotated label for the node.
 */
class LabelMismatchError(
    private val node: HasSourceLocation,
    private val actualLabel: Label,
    private val expectedLabel: Label
) : InformationFlowError() {
    override val category: String
        get() = "Information Flow Label Mismatch"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This term does not have the label I expect:")
                .withSource(node.sourceLocation) /
                Document("I inferred its label as:")
                    .withData(actualLabel) /
                Document("But its label should be:")
                    .withData(expectedLabel)
}
