package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name

/**
 * Thrown when a function or a method is given too many or too few arguments.
 *
 * @param expected The number of arguments expected by the method.
 * @param actual The arguments that are given to the method.
 * */
class IncorrectNumberOfArgumentsError(
    private val method: Located<Name>,
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
        get() = method.sourceLocation.sourcePath

    override val description: Document
        get() {
            val argumentsPlural = Document(if (expected == 1) "argument" else "arguments")
            return Document("Call to") * method *
                "expects" * Document("$expected") * argumentsPlural +
                Document(", but it got") * Document("${actual.size}") * Document("instead.")
                .withSource(method.sourceLocation)
        }
}
