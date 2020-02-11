package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.types.Type
import java.io.PrintStream

/**
 * Thrown when the inferred type of an AST node does not match its expected type.
 *
 * @param node node that has the incorrect type
 * @param actualType inferred type for the node
 * @param expectedType type the node should have
 */
class TypeMismatchError(
    private val node: HasSourceLocation,
    private val actualType: Type,
    private val expectedType: Type
) : CompilationError() {
    override val category: String
        get() = "Type Mismatch"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override fun print(output: PrintStream) {
        super.print(output)

        output.println("This term does not have the type I expect:")

        output.println()
        node.sourceLocation.showInSource(output)

        output.println("It has type:")
        output.println()
        addIndentation(output)
        output.print(actualType) // TODO: Printer.run(actualType, output)
        output.println()

        output.println()
        output.println("But it should have type:")
        output.println()
        addIndentation(output)
        output.print(expectedType) // TODO: Printer.run(expectedType, output)
        output.println()

        output.println()
    }
}
