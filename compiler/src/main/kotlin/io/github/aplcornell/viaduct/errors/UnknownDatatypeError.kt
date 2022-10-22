package io.github.aplcornell.viaduct.errors

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.syntax.ObjectVariableNode
import io.github.aplcornell.viaduct.syntax.datatypes.ClassName

/**
 * Thrown when an object is declared with an unknown datatype (class name).
 *
 * @param objectName Receiver of the method.
 * @param className The unrecognized method.
 */
class UnknownDatatypeError(
    private val objectName: ObjectVariableNode,
    private val className: ClassName
) : CompilationError() {
    override val category: String
        get() = "Unknown Datatype"

    override val source: String
        get() = objectName.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("This object has an unknown datatype named") * className + Document(":")
                .withSource(objectName.sourceLocation)
}
