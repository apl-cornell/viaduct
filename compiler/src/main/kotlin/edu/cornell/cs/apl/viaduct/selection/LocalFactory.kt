package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class LocalFactory(program: ProgramNode) : ProtocolFactory {
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    private val protocols: List<SpecializedProtocol> = run {
        val hostTrustConfiguration = HostTrustConfiguration(program)
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        hosts.map(::Local).map { SpecializedProtocol(it, hostTrustConfiguration) }
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
}
