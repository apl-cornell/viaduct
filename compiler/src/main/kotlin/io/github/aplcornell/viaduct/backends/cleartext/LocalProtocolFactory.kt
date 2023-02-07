package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode

class LocalProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: Set<Protocol> = program.hosts.map(::Local).toSet()

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = protocols
}
