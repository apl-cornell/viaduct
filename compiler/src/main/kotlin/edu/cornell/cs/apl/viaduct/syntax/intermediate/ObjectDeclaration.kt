package edu.cornell.cs.apl.viaduct.syntax.intermediate

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariableNode
import edu.cornell.cs.apl.viaduct.syntax.ValueTypeNode

interface ObjectDeclaration {
    val name: ObjectVariableNode
    val className: ClassNameNode
    val typeArguments: Arguments<ValueTypeNode>
    val labelArguments: Arguments<Located<Label>>?
    val objectDeclarationAsNode: Node
}
