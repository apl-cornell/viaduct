package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.selection.ProtocolSelector
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class SimpleSelector(nameAnalysis: NameAnalysis, val informationFlowAnalysis: InformationFlowAnalysis) : ProtocolSelector {
    private val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 2 }
    private val protocols: List<SpecializedProtocol> =
        (hosts.map(::Local) + hostSubsets.map(::Replication) + hostSubsets.map(::MPCWithAbort))
            .map { SpecializedProtocol(it, hostTrustConfiguration) }

    override fun selectLet(assignment: Map<Variable, Protocol>, node: LetNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
    }

    override fun selectDeclaration(assignment: Map<Variable, Protocol>, node: DeclarationNode): Set<Protocol> {
        return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }.toSet()
    }
}

fun simpleProtocolSort(p: Protocol): Int {
    return when (p) {
        is Local -> 0
        is Replication -> 1
        is MPCWithAbort -> 2
        else -> 10
    }
}

/** A [Protocol] specialized to a [HostTrustConfiguration] that caches [Protocol.authority]. */
private class SpecializedProtocol(
    val protocol: Protocol,
    hostTrustConfiguration: HostTrustConfiguration
) {
    val authority: Label = protocol.authority(hostTrustConfiguration)
}
