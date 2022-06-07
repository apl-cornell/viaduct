package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.div
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.apl.prettyprinting.times
import io.github.apl_cornell.apl.prettyprinting.tupled
import io.github.apl_cornell.viaduct.syntax.MethodNameNode
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.types.ObjectType
import io.github.apl_cornell.viaduct.syntax.types.ValueType

/**
 * Thrown when an object is called with a method it does not recognize.
 *
 * @param objectName Receiver of the method.
 * @param methodName The unrecognized method.
 * @param objectType Inferred type of the object.
 * @param argumentTypes Types of the arguments the method was called with.
 */
class UnknownMethodError(
    private val objectName: ObjectVariableNode,
    private val methodName: MethodNameNode,
    private val objectType: ObjectType? = null,
    private val argumentTypes: List<ValueType>? = null
) : CompilationError() {
    override val category: String
        get() = "Unknown Method"

    override val source: String
        get() = objectName.sourceLocation.sourcePath

    override val description: Document
        get() =
            if (objectType != null && argumentTypes != null) {
                Document("This object does not have a method named") * methodName + Document(":")
                    .withSource(objectName.sourceLocation) /
                    Document("The object's type is:").withData(objectType) /
                    Document("And the method's signature is:").withData(methodName + argumentTypes.tupled())
            } else {
                Document("This object does not have a method named") * methodName + Document(":")
                    .withSource(objectName.sourceLocation)
            }
}
