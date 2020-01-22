package edu.cornell.cs.apl.viaduct.syntax.surface

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A list of arguments. */
class Arguments(arguments: List<ExpressionNode>) : List<ExpressionNode> by arguments {
    // Create an immutable copy
    val arguments: List<ExpressionNode> = arguments.toPersistentList()

    constructor(vararg arguments: ExpressionNode) : this(persistentListOf(*arguments))
}
