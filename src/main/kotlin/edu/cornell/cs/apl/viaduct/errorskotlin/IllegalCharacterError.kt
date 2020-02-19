package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

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

    override val description: Document
        get() =
            Document("I ran into a character I did not expect:").withSource(location)
}
