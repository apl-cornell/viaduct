package io.github.aplcornell.viaduct.reordering

import io.github.aplcornell.viaduct.attributes.attribute
import io.github.aplcornell.viaduct.precircuitanalysis.NameAnalysis
import io.github.aplcornell.viaduct.syntax.precircuit.BlockNode
import io.github.aplcornell.viaduct.syntax.precircuit.ComputeLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.LetNode
import io.github.aplcornell.viaduct.syntax.precircuit.Node
import io.github.aplcornell.viaduct.syntax.precircuit.ProgramNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReturnNode
import io.github.aplcornell.viaduct.syntax.precircuit.StatementNode
import io.github.aplcornell.viaduct.syntax.precircuit.VariableReferenceNode

val Node.uses: List<VariableReferenceNode> by attribute { TODO() }

class DependencyGraph(program: ProgramNode) {
    private val nameAnalysis: NameAnalysis = NameAnalysis.get(program)

    fun BlockNode<StatementNode>.buildDependencyGraph(block: BlockNode<StatementNode>): Map<StatementNode, List<StatementNode>> {
        val dependencyGraph = block.statements.associateWith { listOf<StatementNode>() }.toMutableMap()

        this.forEach { stmt ->
            when (stmt) {
                is ComputeLetNode -> {
                    val dataDeps = stmt.uses.map { nameAnalysis.declaration(it) }
                }
                is LetNode -> TODO()
                is ReturnNode -> TODO()
            }

        }
        /*
        for each statement in block
            data dependencies = declarations(stmt)
            if stmt is output, iodependencies = all previous inputs (this can be relaxed but let's do something easy for now)
            if stmt is declassify, securitydependencies = all previous endorses
            if stmt is endorse, securitydependencies = all previous declassifies
                if bob happy reveal data
                endorse bob input
                cannot move endorse before the if??
         */
        return dependencyGraph
    }
}
