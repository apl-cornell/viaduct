package io.github.aplcornell.viaduct.backends.commitment

import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.analysis.immediateRHS
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.aplcornell.viaduct.util.subsequences

class CommitmentProtocolFactory(val program: ProgramNode) : ProtocolFactory {
    private val nameAnalysis = program.analyses.get<NameAnalysis>()

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
                    updateNode.update.value == io.github.aplcornell.viaduct.syntax.datatypes.Set
                }
            }

            is ParameterNode ->
                true

            else -> false
        }

    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> =
        if (node.isApplicable()) protocols else setOf()
}
