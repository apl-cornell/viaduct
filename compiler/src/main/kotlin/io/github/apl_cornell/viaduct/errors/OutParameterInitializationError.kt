package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.div
import io.github.apl_cornell.viaduct.syntax.intermediate.Node
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode

class OutParameterInitializationError(
    private val parameter: ParameterNode,
    private val useSite: Node? = null
) : CompilationError() {
    override val category: String
        get() = "Out Parameter Initialization Error"

    override val source: String
        get() = parameter.sourceLocation.sourcePath

    override val description: Document
        get() {
            return if (useSite == null) {
                Document("Failed to initialize out parameter exactly once before function return:")
                    .withSource(parameter.sourceLocation)
            } else {
                Document("Failed to initialize out parameter exactly once:")
                    .withSource(parameter.sourceLocation) /
                    Document("Before the following use site:")
                        .withSource(useSite.sourceLocation)
            }
        }
}
