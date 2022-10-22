package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.ValueTypeNode
import io.github.apl_cornell.viaduct.syntax.types.Type

class ArrayType(
    val elementType: ValueTypeNode,
    val shape: Arguments<IndexExpressionNode>,
) : Type {
    override fun toDocument(): Document = elementType + shape.bracketed()
}
