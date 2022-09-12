package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.ValueTypeNode
import io.github.apl_cornell.viaduct.syntax.types.Type
import io.github.apl_cornell.viaduct.syntax.types.ValueType
import kotlinx.collections.immutable.PersistentList

class ArrayType(
    val elementType: ValueType,
    val shape: PersistentList<IndexExpressionNode>,
) : Type {
    override fun toDocument(): Document = elementType + shape.bracketed()
}

class ArrayTypeNode(
    val elementType: ValueTypeNode,
    val shape: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation,
) : Node() {
    override fun toDocument(): Document = elementType + shape.bracketed()
}
