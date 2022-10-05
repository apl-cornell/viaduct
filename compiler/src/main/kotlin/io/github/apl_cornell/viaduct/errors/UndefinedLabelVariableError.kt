package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.syntax.LabelVariable
import io.github.apl_cornell.viaduct.syntax.SourceLocation

class UndefinedLabelVariableError(
    private val labelVariable: LabelVariable,
    private val location: SourceLocation
) : CompilationError() {
    override val category: String
        get() = "Naming Error"

    override val source: String
        get() = location.sourcePath

    override val description: Document
        get() = (Document("Label Parameter") * labelVariable.name * Document("is undeclared"))
            .withSource(location)
}
