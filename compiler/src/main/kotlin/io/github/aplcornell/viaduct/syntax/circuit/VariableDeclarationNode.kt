package io.github.aplcornell.viaduct.syntax.circuit

/** A node that declares a [Variable]. */
sealed interface VariableDeclarationNode {
    /** The variable declared by this node. */
    val name: VariableNode
}
