package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.viaduct.syntax.SourceLocation

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
