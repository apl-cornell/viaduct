package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

class RuntimeError(
    val information: String,
    val node: HasSourceLocation? = null
) : CompilationError() {
    override val category = "Runtime Error"

    override val source: String = node?.sourceLocation?.sourcePath ?: ""

    override val description: Document
        get() = if (node == null) (Document(information)) else Document(information).withSource(node.sourceLocation)
}
