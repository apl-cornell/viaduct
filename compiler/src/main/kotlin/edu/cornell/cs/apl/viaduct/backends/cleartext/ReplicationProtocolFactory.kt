package edu.cornell.cs.apl.viaduct.backends.cleartext

import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class ReplicationProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: Set<Protocol> =
        program.hosts.sorted().subsequences().filter { it.size >= 2 }.map { Replication(it.toSet()) }.toSet()

    override fun viableProtocols(node: LetNode): Set<Protocol> = protocols

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> = protocols

    override fun viableProtocols(node: ParameterNode): Set<Protocol> = protocols
}
