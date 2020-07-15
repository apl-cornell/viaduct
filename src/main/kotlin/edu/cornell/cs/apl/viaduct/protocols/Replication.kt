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
import edu.cornell.cs.apl.viaduct.syntax.specialize
import edu.cornell.cs.apl.viaduct.util.subsequences

/**
 * The protocol that replicates data and computations across a set of hosts in the clear.
 *
 * Replication increases integrity, but doing it in the clear sacrifices confidentiality.
 * Additionally, availability is lost if _any_ participating host aborts.
 */
class Replication(hosts: Set<Host>) : Protocol, SymmetricProtocol(hosts) {
    init {
        require(hosts.size >= 2)
    }

    companion object {
        val protocolName = "Replication"
    }

    override val protocolName: String
        get() = Replication.protocolName

    override fun authority(hostTrustConfiguration: HostTrustConfiguration): Label =
        hosts.map { hostTrustConfiguration(it) }.reduce(Label::meet)

    override fun equals(other: Any?): Boolean =
        other is Replication && this.hosts == other.hosts

    override fun hashCode(): Int =
        hosts.hashCode()
}
class ReplicationSelector(
    val hostTrustConfiguration: HostTrustConfiguration,
    val informationFlowAnalysis: InformationFlowAnalysis
) : ProtocolSelector {
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 2 }
    private val protocols: List<SpecializedProtocol> =
        hostSubsets.map(::Replication).map { it.specialize(hostTrustConfiguration) }

    override fun selectLet(assignment: Map<Variable, Protocol>, node: LetNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }

    override fun selectDeclaration(assignment: Map<Variable, Protocol>, node: DeclarationNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
            .toSet()
    }
}
