package edu.cornell.cs.apl.viaduct.backends.cleartext

import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.VariableDeclarationNode

class LocalProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: Set<Protocol> = program.hosts.map(::Local).toSet()

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = protocols
}
