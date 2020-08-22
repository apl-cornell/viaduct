package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode

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
