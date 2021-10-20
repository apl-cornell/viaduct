package edu.cornell.cs.apl.viaduct.backends.commitment

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.immediateRHS
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.util.subsequences

class CommitmentProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)

    private val protocols: Set<Protocol> = run {
        val hostSubsets = program.hosts.sorted().subsequences().map { it.toSet() }
        hostSubsets.filter { it.size >= 2 }.flatMap { ss -> ss.map { h -> Commitment(h, ss - h) } }.toSet()
    }

    private fun Node.isApplicable(): Boolean {
        return when (this) {
            is LetNode -> nameAnalysis.readers(this).all {
                it.immediateRHS().all {
                    (it is AtomicExpressionNode) || (it is DowngradeNode)
                }
            } && ((this.value is AtomicExpressionNode) || (this.value is DowngradeNode) || (this.value is QueryNode))

            is DeclarationNode -> {
                nameAnalysis.updaters(this).filter { updateNode ->
                    updateNode.update.value != edu.cornell.cs.apl.viaduct.syntax.datatypes.Set
                }.isEmpty()
            }

            else -> false
        }
    }

    override fun viableProtocols(node: LetNode): Set<Protocol> =
        if (node.isApplicable()) protocols else setOf()

    override fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        if (node.isApplicable()) protocols else setOf()

    override fun viableProtocols(node: ParameterNode): Set<Protocol> = protocols
}
