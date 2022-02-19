package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.ObjectTypeNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.VariableNode

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

// TODO: this should be a sealed class.
/** A node that declares an [ObjectVariable]. */
interface ObjectDeclaration : VariableDeclarationNode {
    override val name: ObjectVariableNode
    val objectType: ObjectTypeNode
    val declarationAsNode: Node
}
