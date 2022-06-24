package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.braced
import io.github.apl_cornell.apl.prettyprinting.bracketed
import io.github.apl_cornell.apl.prettyprinting.joined
import io.github.apl_cornell.apl.prettyprinting.nested
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.apl_cornell.viaduct.syntax.datatypes.MutableCell

/** A class name applied to type and label arguments. */
class ObjectTypeNode(
    val className: ClassNameNode,
    val typeArguments: Arguments<ValueTypeNode>,
    val labelArguments: Arguments<LabelNode>?
) : HasSourceLocation, PrettyPrintable {
    override val sourceLocation: SourceLocation
        get() = className.sourceLocation.merge((labelArguments ?: typeArguments).sourceLocation)

    override fun toDocument(): Document {
        val labels = labelArguments?.braced()?.nested() ?: Document()
        return if (className.value == ImmutableCell || className.value == MutableCell) {
            val types = typeArguments.joined().nested()
            types + labels
        } else {
            val types = typeArguments.bracketed().nested()
            className + types + labels
        }
    }

    /** Pretty prints this object type along with protocol annotation. */
    fun toDocument(protocol: ProtocolNode?): Document =
        toDocument() + (protocol?.let { Document("@") + it.value } ?: Document(""))
}
