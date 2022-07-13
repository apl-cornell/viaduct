package io.github.apl_cornell.viaduct.syntax.intermediate2

import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.Variable

/** A node that declares a [Variable]. */
sealed interface VariableDeclarationNode {
    /** The variable declared by this node. */
    val name: VariableNode

    /** The protocol that should store the declared variable. */
    val protocol: ProtocolNode?
}
