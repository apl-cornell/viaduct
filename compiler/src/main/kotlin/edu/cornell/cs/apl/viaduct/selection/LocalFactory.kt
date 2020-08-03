package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

class LocalFactory(
    val hostTrustConfiguration: HostTrustConfiguration,
    val informationFlowAnalysis: InformationFlowAnalysis
) : ProtocolFactory {
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val protocols: List<SpecializedProtocol> =
        hosts.map(::Local).map { SpecializedProtocol(it, hostTrustConfiguration) }

    override fun viableProtocols(node: LetNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }
}
