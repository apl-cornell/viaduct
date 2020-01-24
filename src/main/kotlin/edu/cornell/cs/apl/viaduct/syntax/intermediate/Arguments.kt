package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Temporary
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

/** A list of arguments. Note that arguments are atomic expressions. */
class Arguments(arguments: List<AtomicExpressionNode>) : List<AtomicExpressionNode> by arguments {
    // Make an immutable copy
    val arguments: List<AtomicExpressionNode> = arguments.toPersistentList()

    val reads: Set<Temporary> =
        arguments
            .filterIsInstance<ReadNode>()
            .map { readNode -> readNode.temporary }
            .toPersistentSet()

    constructor(vararg arguments: AtomicExpressionNode) : this(persistentListOf(*arguments))
}
