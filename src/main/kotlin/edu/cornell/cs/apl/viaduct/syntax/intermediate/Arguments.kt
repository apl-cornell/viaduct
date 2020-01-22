package edu.cornell.cs.apl.viaduct.syntax.intermediate

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** A list of arguments. Note that arguments are atomic expressions. */
class Arguments(arguments: List<AtomicExpressionNode>) : List<AtomicExpressionNode> by arguments {
    // Create an immutable copy
    val arguments: List<AtomicExpressionNode> = arguments.toPersistentList()

    constructor(vararg arguments: AtomicExpressionNode) : this(persistentListOf(*arguments))
}
