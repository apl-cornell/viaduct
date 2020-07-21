package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

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
