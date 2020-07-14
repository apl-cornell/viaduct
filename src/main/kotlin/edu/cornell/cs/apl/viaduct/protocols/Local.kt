package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelector
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

/**
 * The protocol that executes code on a specific host in the clear.
 *
 * This protocol has exactly the authority and the capabilities of the host it is tied to.
 */
data class Local(val host: Host) : Protocol, SymmetricProtocol(setOf(host)) {
    companion object {
        val protocolName = "Local"
    }

    override val protocolName: String
        get() = Local.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hostTrustConfiguration(host)
}

class LocalSelector(
    val hostTrustConfiguration: HostTrustConfiguration,
    val informationFlowAnalysis: InformationFlowAnalysis
) : ProtocolSelector {
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val protocols: List<SpecializedProtocol> =
        hosts.map(::Local).map { SpecializedProtocol(it, hostTrustConfiguration) }

    override fun selectLet(assignment: Map<Variable, Protocol>, node: LetNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }

    override fun selectDeclaration(assignment: Map<Variable, Protocol>, node: DeclarationNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }
}
