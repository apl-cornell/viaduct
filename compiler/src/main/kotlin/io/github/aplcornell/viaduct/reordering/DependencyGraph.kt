package io.github.aplcornell.viaduct.reordering

import io.github.aplcornell.viaduct.precircuitanalysis.NameAnalysis
import io.github.aplcornell.viaduct.syntax.precircuit.ArrayTypeNode
import io.github.aplcornell.viaduct.syntax.precircuit.BlockNode
import io.github.aplcornell.viaduct.syntax.precircuit.BreakNode
import io.github.aplcornell.viaduct.syntax.precircuit.CommandLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.ComputeLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.DeclassificationNode
import io.github.aplcornell.viaduct.syntax.precircuit.DowngradeNode
import io.github.aplcornell.viaduct.syntax.precircuit.EndorsementNode
import io.github.aplcornell.viaduct.syntax.precircuit.ExpressionNode
import io.github.aplcornell.viaduct.syntax.precircuit.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.precircuit.IfNode
import io.github.aplcornell.viaduct.syntax.precircuit.InputNode
import io.github.aplcornell.viaduct.syntax.precircuit.LiteralNode
import io.github.aplcornell.viaduct.syntax.precircuit.LookupNode
import io.github.aplcornell.viaduct.syntax.precircuit.LoopNode
import io.github.aplcornell.viaduct.syntax.precircuit.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.precircuit.OutputNode
import io.github.aplcornell.viaduct.syntax.precircuit.ProgramNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReduceNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReferenceNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReturnNode
import io.github.aplcornell.viaduct.syntax.precircuit.StatementNode
import io.github.aplcornell.viaduct.syntax.precircuit.VariableReferenceNode

class DependencyGraph(program: ProgramNode) {
    private val nameAnalysis: NameAnalysis = NameAnalysis.get(program)
    private val nodeToDependencies = mutableMapOf<StatementNode, MutableList<StatementNode>>()
    private val nodeToDependents = mutableMapOf<StatementNode, MutableList<StatementNode>>()

    init {
        program.declarations.filterIsInstance<FunctionDeclarationNode>().forEach { buildDependencyGraph(it.body) }
    }

    private fun uses(node: StatementNode): List<VariableReferenceNode> = when (node) {
        is ComputeLetNode -> uses(node.type) + uses(node.value)
        is CommandLetNode -> when (val command = node.command) {
            is InputNode -> uses(command.type)
            is OutputNode -> uses(command.type) + listOf(command.message)
            is DowngradeNode -> uses(command.expression)
        }
        is ReturnNode -> node.values.flatMap { uses(it) }
        is BreakNode -> listOf()
        is IfNode -> uses(node.guard) + node.thenBranch.flatMap { uses(it) } + node.elseBranch.flatMap { uses(it) }
        is LoopNode -> node.body.flatMap { uses(it) }
    }

    private fun uses(node: ExpressionNode): List<VariableReferenceNode> = when (node) {
        is LiteralNode -> listOf()
        is ReferenceNode -> listOf(node)
        is LookupNode -> listOf(node) + node.indices.flatMap { uses(it) }
        is OperatorApplicationNode -> node.arguments.flatMap { uses(it) }
        is ReduceNode -> uses(node.defaultValue) + uses(node.body)
    }

    private fun uses(node: ArrayTypeNode): List<VariableReferenceNode> = node.shape.flatMap { uses(it) }

    private fun addDependencies(node: StatementNode, dependencies: List<StatementNode>) {
        nodeToDependencies.getOrPut(node) { mutableListOf() }
        nodeToDependents.getOrPut(node) { mutableListOf() }
        dependencies.forEach {
            nodeToDependencies[node]!!.add(it)
            nodeToDependents.getOrPut(it) { mutableListOf() }.add(node)
        }
    }

    fun dependents(statement: StatementNode) = nodeToDependents[statement]!!
    fun dependentsClosure(statement: StatementNode): Set<StatementNode> {
        val seen = mutableSetOf<StatementNode>()
        val frontier = arrayListOf<StatementNode>()
        frontier.addAll(nodeToDependents[statement]!!)
        while (frontier.isNotEmpty()) {
            val curr = frontier.removeAt(0)
            if (curr !in seen) {
                seen.add(curr)
                frontier.addAll(nodeToDependents[curr]!!)
            }
        }
        return seen
    }

    fun dependencies(statement: StatementNode): List<StatementNode> {
        return nodeToDependencies[statement]!!
    }

    private fun dataDependencies(stmt: StatementNode): List<StatementNode> {
        return uses(stmt).map { nameAnalysis.declaration(it) }.mapNotNull {
            // TODO Extremely hacky. A better way is to have two Contexts in NameAnalysis;
            //  one for contextAfter (let nodes only) and one for contextChildren (other variable declaration types)
            when (it) {
                is StatementNode -> it
                else -> null
            }
        }
    }

    /** Fills in [nodeToDependencies] for [this]. Reordering will only occur within blocks. */
// TODO Kind of weird: Data dependency edges extend outside of blocks but we only include security dependencies
//  which are within the same block due to our assumption that we'll only reorder within blocks
    private fun buildDependencyGraph(block: BlockNode<StatementNode>) {
        val prevInputs: List<StatementNode> = listOf()
        val prevOutputs: List<StatementNode> = listOf()
        val prevDeclassifies: List<StatementNode> = listOf()
        val prevEndorses: List<StatementNode> = listOf()
        block.forEach { stmt ->
            addDependencies(stmt, dataDependencies(stmt))
            when (stmt) {
                is ComputeLetNode -> {
                    addDependencies(stmt, listOf())
                } // Only data dependencies matter
                is CommandLetNode -> {
                    when (stmt.command) {
                        is InputNode, is OutputNode ->
                            // Shouldn't change interface to the user
                            addDependencies(stmt, prevInputs + prevOutputs)

                        is DeclassificationNode -> addDependencies(stmt, prevEndorses)
                        is EndorsementNode -> {
                            addDependencies(stmt, listOf())
                        } // TODO No need to depend on declassifies? Does it break robust declassification
                    }
                }
                is ReturnNode -> // Cannot reorder with any side effects
                    addDependencies(stmt, prevInputs + prevOutputs + prevDeclassifies + prevEndorses)
                is BreakNode ->
                    // Cannot reorder with any side effects
                    addDependencies(stmt, prevInputs + prevOutputs + prevDeclassifies + prevEndorses)
                is IfNode -> {  // TODO: the if's dependencies should be the union of its childrens
                    addDependencies(stmt, listOf())
                    buildDependencyGraph(stmt.thenBranch)
                    buildDependencyGraph(stmt.elseBranch)
                }
                is LoopNode -> {
                    addDependencies(stmt, listOf())
                    buildDependencyGraph(stmt.body)
                }
            }
        }
    }
}
