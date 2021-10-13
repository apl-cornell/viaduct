package edu.cornell.cs.apl.viaduct.backends.cleartext

import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class ReplicationFactory(val program: ProgramNode) : ProtocolFactory {
    val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 2 }
        hostSubsets.map(::Replication).map { SpecializedProtocol(it, hostTrustConfiguration) }
    }

    override fun protocols() = protocols

    override fun availableProtocols(): Set<ProtocolName> = setOf(Replication.protocolName)

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()

    override fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocols.map { it.protocol }.toSet()
}
