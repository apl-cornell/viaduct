package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

// TODO: I don't think there is an example that throws this error...

/**
 * Thrown when the inferred label of [node] does nat match its annotated label.
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
                .withSource(node.sourceLocation) +
                Document("I inferred its label as:")
                    .withData(actualLabel) +
                Document("But its label should be:")
                    .withData(expectedLabel)
}
