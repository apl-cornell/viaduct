package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType

/**
 * Thrown when the inferred type of an AST node does not match its expected type.
 *
 * @param node node that has the incorrect type
 * @param actualType inferred type for the node
 * @param expectedType type the node should have
 */
class TypeMismatchError(
    private val node: HasSourceLocation,
    private val actualType: ValueType,
    private val expectedType: ValueType
) : CompilationError() {
    override val category: String
        get() = "Type Mismatch"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This term does not have the type I expect:")
                .withSource(node.sourceLocation) +
                Document("It has type:").withData(actualType) +
                Document("But it should have type:").withData(expectedType)
}
