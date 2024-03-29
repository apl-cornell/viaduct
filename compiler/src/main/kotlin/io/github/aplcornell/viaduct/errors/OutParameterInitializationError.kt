package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.div
import io.github.aplcornell.viaduct.syntax.intermediate.Node
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode

class OutParameterInitializationError(
    private val parameter: ParameterNode,
    private val useSite: Node? = null,
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
