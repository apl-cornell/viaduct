package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.viaduct.syntax.SourceLocation
import java.io.PrintStream

/**
 * Thrown when the lexer encounters an illegal character.
 *
 * @param location Source location of the illegal character.
 */
class IllegalCharacterError(private val location: SourceLocation) : CompilationError() {
    override val category: String
        get() = "Parse Error"

    override val source: String
        get() = location.sourcePath

    override fun print(output: PrintStream) {
        super.print(output)

        output.println("I ran into a character I did not expect:")
        output.println()

        location.showInSource(output)
    }
}
