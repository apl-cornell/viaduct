package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

class InvalidJumpError(
    private val jumpLocation: SourceLocation
) : CompilationError() {
    override val category: String
        get() = "Invalid Jump"

    override val source: String
        get() = jumpLocation.sourcePath

    override val description: Document
        get() = Document("Invalid jump found here:").withSource(jumpLocation)
}
