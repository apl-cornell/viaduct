package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.SourceLocation

/** Thrown when a [Name] has multiple declarations. */
class NameClashError(
    private val name: Name,
    private val firstDeclaration: SourceLocation,
    private val secondDeclaration: SourceLocation
) : CompilationError() {
    init {
        require(this.firstDeclaration.sourcePath == this.secondDeclaration.sourcePath)
    }

    override val category: String
        get() = "Name Clash"

    override val source: String
        get() = firstDeclaration.sourcePath

    override val description: Document
        get() =
            Document("There are multiple declarations of") * name + Document(".") *
                Document("One here:")
                    .withSource(firstDeclaration) /
                Document("And another one here:")
                    .withSource(secondDeclaration)
}
