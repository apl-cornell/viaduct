package io.github.apl_cornell.viaduct.syntax.intermediate

import io.github.apl_cornell.viaduct.syntax.ObjectVariable
import io.github.apl_cornell.viaduct.syntax.ObjectVariableNode
import io.github.apl_cornell.viaduct.syntax.ProtocolNode
import io.github.apl_cornell.viaduct.syntax.Variable
import io.github.apl_cornell.viaduct.syntax.VariableNode

/** A node that declares a [Variable]. */
sealed interface VariableDeclarationNode {
    /** The variable declared by this node. */
    val name: VariableNode

    /** The protocol that should store the declared variable. */
    val protocol: ProtocolNode?
}

/** A node that declares an [ObjectVariable]. */
sealed interface ObjectVariableDeclarationNode : VariableDeclarationNode {
    override val name: ObjectVariableNode
}
