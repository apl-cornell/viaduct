package io.github.aplcornell.viaduct.reordering

import io.github.aplcornell.viaduct.precircuitanalysis.protocols
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.precircuit.BlockNode
import io.github.aplcornell.viaduct.syntax.precircuit.BreakNode
import io.github.aplcornell.viaduct.syntax.precircuit.CommandLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.ComputeLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.ControlFlowBlockNode
import io.github.aplcornell.viaduct.syntax.precircuit.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.precircuit.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.precircuit.IfNode
import io.github.aplcornell.viaduct.syntax.precircuit.LoopNode
import io.github.aplcornell.viaduct.syntax.precircuit.ProgramNode
import io.github.aplcornell.viaduct.syntax.precircuit.ReturnNode
import io.github.aplcornell.viaduct.syntax.precircuit.RoutineBlockNode
import io.github.aplcornell.viaduct.syntax.precircuit.StatementNode

class Reorder(private val programNode: ProgramNode) {
    private val dependencyGraph = DependencyGraph(programNode)

    /** Heuristically (based on the desired [protocol]) fetch the next statement from [ready] to be processed */
    private fun fetchNext(ready: Set<StatementNode>, protocol: Protocol?): StatementNode {
        if (protocol != null) {
            val sameProtocolStatements = ready.filter { protocol in it.protocols() }
            if (sameProtocolStatements.isNotEmpty()) {
                return sameProtocolStatements.first()
            }
        }
        return ready.first()
    }

    /** Returns a re-ordered copy of [statements] through a topological sort with constraints */
    private fun reorderStatements(statements: List<StatementNode>): List<StatementNode> {
        // Statements whose dependencies have already been computed
        val ready = statements.filter { dependencyGraph.dependencies(it).isEmpty() }.toMutableSet()
        val processed = ArrayDeque<StatementNode>()
        var protocol: Protocol? = null
        while (ready.isNotEmpty()) {
            val curr = fetchNext(ready = ready, protocol = protocol)
            ready.remove(curr)
            processed.add(curr)
            protocol = curr.protocols().firstOrNull()
            ready.addAll(dependencyGraph.dependents(curr)
                .filter { dependencyGraph.dependencies(it).minus(processed).isEmpty() })
        }
        return processed.toList()
    }

    /** Returns a re-ordered copy of [blockNode] */
    private fun reorder(blockNode: BlockNode<*>): BlockNode<StatementNode> = when (blockNode) {
        is ControlFlowBlockNode<*> -> ControlFlowBlockNode(
            statements = reorderStatements(blockNode.statements.map { reorder(it) }),
            // TODO since we reorder the children and make new nodes, they're no longer in the dependency map. bug
            sourceLocation = blockNode.sourceLocation
        )
        is RoutineBlockNode<*> -> RoutineBlockNode(
            statements = reorderStatements(blockNode.statements.map { reorder(it) }),
            returnStatement = blockNode.returnStatement,
            sourceLocation = blockNode.sourceLocation
        )
    }

    private fun reorder(node: StatementNode): StatementNode = when (node) {
        is LoopNode -> LoopNode(
            body = reorder(node.body), sourceLocation = node.sourceLocation
        )
        is IfNode -> IfNode(
            guard = node.guard,
            thenBranch = reorder(node.thenBranch),
            elseBranch = reorder(node.elseBranch),
            sourceLocation = node.sourceLocation
        )
        is BreakNode -> BreakNode(sourceLocation = node.sourceLocation)
        is ReturnNode -> ReturnNode(values = node.values, sourceLocation = node.sourceLocation)
        is CommandLetNode, is ComputeLetNode -> node
    }

    /** Returns a copy of the Program with re-ordered Blocks */
    fun applyReorder(): ProgramNode {
        val newFunctions = programNode.declarations.filterIsInstance<FunctionDeclarationNode>().map {
            FunctionDeclarationNode(it.name, it.sizes, it.inputs, it.outputs, reorder(it.body), it.sourceLocation)
        }

        println(newFunctions)

        return ProgramNode(
            declarations = programNode.declarations.filterIsInstance<HostDeclarationNode>() + newFunctions,
            sourceLocation = programNode.sourceLocation
        )
    }
}
