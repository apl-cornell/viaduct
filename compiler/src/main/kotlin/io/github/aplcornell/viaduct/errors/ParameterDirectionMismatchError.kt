package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode

class ParameterDirectionMismatchError(
    private val parameter: ParameterNode,
    private val argument: FunctionArgumentNode
) : CompilationError() {
    override val category: String
        get() = "Parameter Direction Mismatch"

    override val source: String
        get() = parameter.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This argument has the wrong direction:")
                .withSource(argument.sourceLocation) /
                Document("For this parameter:")
                    .withSource(parameter.sourceLocation)
}
