package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

/** Thrown when a [Name] is referenced before it is defined. */
class UndefinedNameError(name: Located<Name>) : CompilationError() {
    private val name: Name = name.value
    private val location: SourceLocation = name.sourceLocation

    override val category: String
        get() = "Naming Error"

    override val source: String
        get() = location.sourcePath

    override val description: Document
        get() =
            // TODO: show similar names in context ("Did you mean: ...")
            (Document("I cannot find") * name.nameCategory * name + ":")
                .withSource(location)
}
