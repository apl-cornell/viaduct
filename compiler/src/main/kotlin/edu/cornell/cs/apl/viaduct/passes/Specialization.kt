package edu.cornell.cs.apl.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.deepCopy
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import java.util.LinkedList

/** Returns an AST where every call site is specialized into new functions as much as possible.
 *  This allows for the most liberal protocol selection possible, at the cost of redundancy.
 *  The specializer will not specialize (mutually) recursive functions to prevent unbounded specialization.
 */
fun ProgramNode.specialize(): ProgramNode {
    val (newMainBlock, newFunctions) = Specializer(this.functionMap, this.main.body).specialize()

    val newDeclarations = mutableListOf<TopLevelDeclarationNode>()
    newDeclarations.addAll(
        this.declarations
            .filterIsInstance<HostDeclarationNode>()
            .map { hostDecl -> hostDecl.deepCopy() as HostDeclarationNode }
    )
    newDeclarations.addAll(newFunctions)
    newDeclarations.add(
        ProcessDeclarationNode(
            this.main.protocol,
            newMainBlock,
            this.main.sourceLocation
        )
    )

    return ProgramNode(newDeclarations, this.sourceLocation)
}

private class Specializer(
    val functionMap: Map<FunctionName, FunctionDeclarationNode>,
    val mainProgram: BlockNode
) {
    val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())

    // maintain a worklist of functions to specialize
    val worklist = LinkedList<Triple<FunctionName, FunctionName, PersistentMap<FunctionName, FunctionName>>>()

    fun specializeStatement(
        callingCtx: PersistentMap<FunctionName, FunctionName>,
        stmt: StatementNode
    ): StatementNode {
        return when (stmt) {
            is FunctionCallNode -> {
                val specializedName = callingCtx[stmt.name.value]

                // there are two possible cases to handle for call sites:
                // - case 1: calling a function already specialized in the calling context (then branch).
                //   here we just call the already specialized version of the function. this "closes the loop" for
                //   (mutually) recursive functions and prevents unbounded specialization.
                // - case 2: calling a function not specialized yet in the calling context.
                //   here we create a new version of the function to specialize and add it to the worklist
                val newName: FunctionName =
                    if (specializedName != null) {
                        specializedName
                    } else { // case 2:
                        val freshName = FunctionName(nameGenerator.getFreshName(stmt.name.value.name))
                        worklist.add(Triple(stmt.name.value, freshName, callingCtx))
                        freshName
                    }

                FunctionCallNode(
                    Located(newName, stmt.name.sourceLocation),
                    Arguments(
                        stmt.arguments.map { arg -> arg.deepCopy() as FunctionArgumentNode },
                        stmt.arguments.sourceLocation
                    ),
                    stmt.sourceLocation
                )
            }

            is IfNode -> {
                IfNode(
                    stmt.guard.deepCopy() as AtomicExpressionNode,
                    specializeStatement(callingCtx, stmt.thenBranch) as BlockNode,
                    specializeStatement(callingCtx, stmt.elseBranch) as BlockNode,
                    stmt.sourceLocation
                )
            }

            is InfiniteLoopNode -> {
                InfiniteLoopNode(
                    specializeStatement(callingCtx, stmt.body) as BlockNode,
                    stmt.jumpLabel,
                    stmt.sourceLocation
                )
            }

            is BlockNode -> {
                BlockNode(
                    stmt.statements.map { child -> specializeStatement(callingCtx, child) },
                    stmt.sourceLocation
                )
            }

            else -> stmt.deepCopy() as StatementNode
        }
    }

    /** Specialize by processing call site in the worklist. */
    fun specialize(): Pair<BlockNode, List<FunctionDeclarationNode>> {
        val newFunctions = mutableListOf<FunctionDeclarationNode>()
        val newMain = specializeStatement(persistentMapOf(), mainProgram) as BlockNode

        while (worklist.isNotEmpty()) {
            val (origName, newName, callingCtx) = worklist.remove()
            val currentCtx = callingCtx.put(origName, newName)
            val function = functionMap[origName]!!
            newFunctions.add(
                FunctionDeclarationNode(
                    Located(newName, function.name.sourceLocation),
                    function.pcLabel,
                    Arguments(
                        function.parameters.map { param -> param.deepCopy() as ParameterNode },
                        function.parameters.sourceLocation
                    ),
                    specializeStatement(currentCtx, function.body) as BlockNode,
                    function.sourceLocation
                )
            )
        }

        return Pair(newMain, newFunctions)
    }
}
