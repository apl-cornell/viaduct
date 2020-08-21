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
import java.util.LinkedList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

fun ProgramNode.specialize(): ProgramNode {
    val (newMainBlock, newFunctions) = specialize(this.functionMap, this.main.body)

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

private fun specialize(
    functionMap: Map<FunctionName, FunctionDeclarationNode>,
    mainProgram: BlockNode
): Pair<BlockNode, List<FunctionDeclarationNode>> {
    val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())
    val newFunctions = mutableListOf<FunctionDeclarationNode>()
    val worklist = LinkedList<Triple<FunctionName, FunctionName, PersistentMap<FunctionName, FunctionName>>>()

    fun specializeStatement(callingCtx: PersistentMap<FunctionName, FunctionName>, stmt: StatementNode): StatementNode {
        return when (stmt) {
            is FunctionCallNode -> {
                val specializedName = callingCtx[stmt.name.value]

                val newName: FunctionName =
                    if (specializedName != null) {
                        specializedName
                    } else {
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
