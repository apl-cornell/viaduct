package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.SourceLocation

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
