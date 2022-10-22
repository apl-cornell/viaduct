package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.aplcornell.viaduct.util.subsequences

class ReplicationProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: Set<Protocol> =
        program.hosts.sorted().subsequences().filter { it.size >= 2 }.map { Replication(it.toSet()) }.toSet()

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = protocols
}
