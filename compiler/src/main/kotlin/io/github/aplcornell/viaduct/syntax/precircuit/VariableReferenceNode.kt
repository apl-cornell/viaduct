package io.github.aplcornell.viaduct.syntax.precircuit

/** A node that declares a [Variable]. */
sealed interface VariableReferenceNode {
    /** The variable referenced by this node. */
    val name: VariableNode
}
