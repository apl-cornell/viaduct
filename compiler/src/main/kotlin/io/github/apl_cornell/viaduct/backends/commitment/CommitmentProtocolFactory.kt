package io.github.apl_cornell.viaduct.backends.commitment

import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.analysis.immediateRHS
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.QueryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.apl_cornell.viaduct.util.subsequences

class CommitmentProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = NameAnalysis.get(program)

    private val protocols: Set<Protocol> = run {
        val hostSubsets = program.hosts.sorted().subsequences().map { it.toSet() }
        hostSubsets.filter { it.size >= 2 }.flatMap { ss -> ss.map { h -> Commitment(h, ss - h) } }.toSet()
    }

    private fun VariableDeclarationNode.isApplicable(): Boolean =
        when (this) {
            is LetNode -> nameAnalysis.readers(this).all { reader ->
                reader.immediateRHS().all {
                    it is AtomicExpressionNode || it is DowngradeNode
                }
            } && (this.value is AtomicExpressionNode || this.value is DowngradeNode || this.value is QueryNode)

            is DeclarationNode -> {
                nameAnalysis.updaters(this).all { updateNode ->
                    updateNode.update.value == io.github.apl_cornell.viaduct.syntax.datatypes.Set
                }
            }

            is ParameterNode ->
                true

            else -> false
        }

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> =
        if (node.isApplicable()) protocols else setOf()
}
