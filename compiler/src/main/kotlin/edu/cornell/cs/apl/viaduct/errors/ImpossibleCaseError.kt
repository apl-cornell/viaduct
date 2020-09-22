package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node

/** Thrown when the compiler reaches an impossible state. */
abstract class ImpossibleCaseError : CompilationError() {
    override val category = "Impossible Case"
}

/**
 * Thrown when a unknown Node is tagged as an ObjectDeclaration.
 *
 * @param node: The unknown node.
 */
class UnknownObjectDeclarationError(
    val node: Node
) : ImpossibleCaseError() {
    override val source: String = node.sourceLocation.sourcePath

    override val description: Document
        get() = Document("Unknown type of object declaration found:")
            .withSource(node.sourceLocation)
}
