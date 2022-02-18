package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.VariableNode

/** A node that declares a [Variable]. */
sealed interface VariableDeclarationNode {
    /** The variable declared by this node. */
    val name: VariableNode

    /** The protocol that should store the declared variable. */
    val protocol: ProtocolNode?
}

// TODO: this should be a sealed class.
/** A node that declares an [ObjectVariable]. */
interface ObjectDeclaration : VariableDeclarationNode {
    override val name: ObjectVariableNode
    val className: ClassNameNode
    val typeArguments: Arguments<ValueTypeNode>
    val labelArguments: Arguments<LabelNode>?
    val declarationAsNode: Node
}
