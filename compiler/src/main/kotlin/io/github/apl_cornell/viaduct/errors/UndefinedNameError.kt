package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.apl.prettyprinting.times
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.Name
import io.github.apl_cornell.viaduct.syntax.SourceLocation

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
