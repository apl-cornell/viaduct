package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.Name
import io.github.apl_cornell.viaduct.syntax.ValueTypeNode
import io.github.apl_cornell.viaduct.syntax.types.Type

typealias VariableNode = Located<Variable>

sealed class Node : HasSourceLocation

abstract class ArrayType(
    val elementType: ValueTypeNode,
    val shape: List<ExpressionNode>,
) : Type

//  Note that scalars are represented as arrays of dimension zero:
//    val x = 5 ===> val x[] = 5
class Variable(override val name: String) : Name {
    override val nameCategory: String
        get() = "variable"

    override fun toDocument(): Document =
        Document(name)
}
