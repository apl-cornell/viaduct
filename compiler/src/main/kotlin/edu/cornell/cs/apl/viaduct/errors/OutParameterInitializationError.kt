package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.div
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode

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
                Document("Failed to initialize out parameter before function return:")
                    .withSource(parameter.sourceLocation)
            } else {
                Document("Failed to initialize out parameter:")
                    .withSource(parameter.sourceLocation) /
                    Document("Before the following use site:")
                        .withSource(useSite.sourceLocation)
            }
        }
}
