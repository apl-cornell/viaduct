package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.immediateRHS
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.SpecializedProtocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.specialize
import edu.cornell.cs.apl.viaduct.util.subsequences

class CommitmentSelector(
    val nameAnalysis: NameAnalysis,
    val hostTrustConfiguration: HostTrustConfiguration,
    val informationFlowAnalysis: InformationFlowAnalysis
) : ProtocolSelector {
    private val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
    private val hostSubsets = hosts.subsequences().map { it.toSet() }.filter { it.size >= 1 }
    private val protocols: List<SpecializedProtocol> =
        hosts.flatMap { h ->
            hostSubsets.flatMap { s ->
                if (s.contains(h)) {
                    listOf()
                } else {
                    listOf(Commitment(h, s).specialize(hostTrustConfiguration))
                }
            }
        }

    private fun LetNode.isApplicable(): Boolean {
        return nameAnalysis.readers(this).all { reader ->
            reader.immediateRHS().all { e ->
                (e is AtomicExpressionNode) || (e is DowngradeNode)
            }
        }
    }

    override fun select(node: LetNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> {
        if (node.isApplicable()) {
            return protocols.filter { it.authority.actsFor(informationFlowAnalysis.label(node)) }.map { it.protocol }
                .toSet()
        } else {
            return setOf()
        }
    }

    override fun select(node: DeclarationNode, currentAssignment: Map<Variable, Protocol>): Set<Protocol> {
        TODO("commitments for objects ")
    }
}
