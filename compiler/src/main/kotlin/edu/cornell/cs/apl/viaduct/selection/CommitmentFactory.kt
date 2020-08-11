package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.immediateRHS
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class CommitmentFactory(program: ProgramNode) : ProtocolFactory {

    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    private val hostTrustConfiguration = HostTrustConfiguration(program)
    private val nameAnalysis = NameAnalysis.get(program)

    private val protocols: List<Protocol> = run {
        val hosts: List<Host> = hostTrustConfiguration.keys.sorted()
        val hostSubsets = hosts.subsequences().map { it.toSet() }
        hostSubsets.filter { it.size >= 2 }.flatMap { ss ->
            ss.map { h ->
                (h to ss.minus(h))
            }
        }.map { Commitment(it.first, it.second) }
    }

    private fun Node.isApplicable(): Boolean {
        return when (this) {
            is LetNode -> nameAnalysis.readers(this).all {
                it.immediateRHS().all {
                    (it is AtomicExpressionNode) || (it is DowngradeNode)
                }
            }
            is DeclarationNode -> nameAnalysis.users(this).all {
                it is QueryNode
            }
            else -> false
        }
    }

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols.toSet()
        } else {
            setOf()
        }
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> {
        return if (node.isApplicable()) {
            protocols.toSet()
        } else {
            setOf()
        }
    }
}
