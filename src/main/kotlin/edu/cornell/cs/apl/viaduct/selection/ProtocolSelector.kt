package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

interface ProtocolSelector {
    fun selectLet(assignment: Map<Variable, Protocol>, node: LetNode): Set<Protocol>
    fun selectDeclaration(assignment: Map<Variable, Protocol>, node: DeclarationNode): Set<Protocol>
}
