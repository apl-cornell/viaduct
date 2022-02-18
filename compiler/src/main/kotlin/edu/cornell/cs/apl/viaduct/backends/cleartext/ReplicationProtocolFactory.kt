package edu.cornell.cs.apl.viaduct.backends.cleartext

import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.VariableDeclarationNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class ReplicationProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: Set<Protocol> =
        program.hosts.sorted().subsequences().filter { it.size >= 2 }.map { Replication(it.toSet()) }.toSet()

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = protocols
}
