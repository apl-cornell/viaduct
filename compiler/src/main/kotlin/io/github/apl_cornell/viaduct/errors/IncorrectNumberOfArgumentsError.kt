package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.apl.prettyprinting.times
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.Name

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
