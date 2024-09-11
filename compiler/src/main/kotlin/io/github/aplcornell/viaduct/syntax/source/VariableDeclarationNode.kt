package io.github.aplcornell.viaduct.syntax.source

/** A node that declares a [Variable]. */
sealed interface VariableDeclarationNode {
    /** The variable declared by this node. */
    val name: VariableNode
}
