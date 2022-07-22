package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.Variable
import io.github.apl_cornell.viaduct.syntax.VariableNode

/** A node that declares a [Variable]. */
sealed interface VariableDeclarationNode {
    /** The variable declared by this node. */
    val variable: VariableNode

    /** The protocol that should store the declared variable. */
    val protocol: ProtocolNode?
}
