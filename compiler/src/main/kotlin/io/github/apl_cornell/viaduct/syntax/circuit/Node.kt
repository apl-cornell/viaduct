package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.prettyprinting.bracketed
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.SourceLocation
import io.github.apl_cornell.viaduct.syntax.ValueTypeNode
import io.github.apl_cornell.viaduct.syntax.types.Type

sealed class Node : HasSourceLocation, PrettyPrintable

abstract class ArrayType(
    val elementType: ValueTypeNode,
    val shape: List<ExpressionNode>,
) : Type

class ArrayTypeNode(
    val elementType: ValueTypeNode,
    val shape: Arguments<IndexExpressionNode>,
    override val sourceLocation: SourceLocation
) : HasSourceLocation, PrettyPrintable {
    override fun toDocument(): Document = elementType + shape.bracketed()
}

//  Note that scalars are represented as arrays of dimension zero:
//    val x = 5 ===> val x[] = 5
