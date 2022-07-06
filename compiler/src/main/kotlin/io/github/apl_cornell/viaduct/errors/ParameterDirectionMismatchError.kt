package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.div
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode

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
