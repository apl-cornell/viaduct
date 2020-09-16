package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode

interface ObjectDeclaration {
    val name: ObjectVariableNode
    val className: ClassNameNode
    val typeArguments: Arguments<ValueTypeNode>
    val labelArguments: Arguments<LabelNode>?
    val declarationAsNode: Node
}
