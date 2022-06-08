package io.github.apl_cornell.viaduct.backends.cleartext

import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.VariableDeclarationNode

class LocalProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: Set<Protocol> = program.hosts.map(::Local).toSet()

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = protocols
}