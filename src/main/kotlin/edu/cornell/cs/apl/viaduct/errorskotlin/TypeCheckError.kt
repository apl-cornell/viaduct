package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.types.Type
import java.io.PrintStream

class TypeCheckError(
    private val node: Node,
    private val actualType: Type,
    private val expectedType: Type
) : CompilationError() {
    override val category: String
        get() = "Type Check"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override fun print(output: PrintStream) {
        super.print(output)

        output.println("The following node has the wrong type:")
        node.sourceLocation.showInSource(output)

        output.print("Actual type: ")
        output.println(actualType.toString())

        output.print("Expected type: ")
        output.println(expectedType.toString())
    }
}
