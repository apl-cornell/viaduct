package io.github.aplcornell.viaduct.syntax.intermediate

import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.ObjectVariableNode
import io.github.aplcornell.viaduct.syntax.ProtocolNode
import io.github.aplcornell.viaduct.syntax.Variable
import io.github.aplcornell.viaduct.syntax.VariableNode

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
