package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.ValueTypeNode

class ArrayTypeNode(
    val elementType: ValueTypeNode,
    val shape: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation,
) : Node() {
    override val children: Iterable<Node>
        get() = shape

    override fun toDocument(): Document = elementType + shape.bracketed()
}
