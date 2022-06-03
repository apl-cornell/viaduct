package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.div
import io.github.apl_cornell.apl.prettyprinting.joined
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * Thrown when the parser runs into an unexpected token.
 *
 * @param location Source location of the unexpected token.
 * @param actualToken Token that was encountered.
 * @param expectedTokens Tokens that would have been valid.
 */
class ParsingError(
    private val location: SourceLocation,
    private val actualToken: String,
    expectedTokens: List<String>
) : CompilationError() {
    private val expectedTokens: ImmutableList<String> = expectedTokens.toPersistentList()

    override val category: String
        get() = "Parse Error"

    override val source: String
        get() = location.sourcePath

    override val description: Document
        get() {
            val expected = expectedTokens.map { Document(it) }.joined()
            return Document("I ran into an issue while parsing this file.")
                .withSource(location) /
                Document("I was expecting one of these:")
                    .withData(expected) /
                Document("Instead, I found:")
                    .withData(Document(actualToken))
        }
}
