package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation

/**
 * Thrown when [node] is given too many or too few arguments.
 *
 * @param expected The number of arguments expected by [node].
 * @param actual The arguments that are given to [node].
 * */
class IncorrectNumberOfArgumentsError(
    private val node: HasSourceLocation,
    private val expected: Int,
    private val actual: Arguments<*>
) : CompilationError() {
    init {
        require(expected != actual.size)
    }

    override val category: String
        get() =
            if (actual.size > expected)
                "Too Many Arguments"
            else
                "Too Few Arguments"

    override val source: String
        get() = node.sourceLocation.sourcePath

    override val description: Document
        get() {
            val argumentsPlural = Document(if (expected == 1) "argument" else "arguments")
            return Document("This node expects") * Document("$expected") * argumentsPlural +
                Document(", but it got") * Document("${actual.size}") * Document("instead.")
                .withSource(node.sourceLocation)
        }
}
