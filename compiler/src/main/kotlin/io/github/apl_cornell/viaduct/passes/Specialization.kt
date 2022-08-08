package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.analysis.main
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionName
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.TopLevelDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.deepCopy
import io.github.apl_cornell.viaduct.util.FreshNameGenerator
import kotlinx.collections.immutable.persistentMapOf
import java.util.LinkedList


//typealias CallingContext = PersistentMap<Pair<FunctionName, List<Label>>, FunctionName>

class CallingContext {

}

/** Returns an AST where every call site is specialized into new functions as much as possible.
 *  This allows for the most liberal protocol selection possible, at the cost of redundancy.
 *  The specializer will not specialize (mutually) recursive functions to prevent unbounded specialization.
 */
fun ProgramNode.specialize(): ProgramNode {
    val main = this.main
    val (newMainBlock, newFunctions) = Specializer(this.functionMap, main.body).specialize()

    val newDeclarations = mutableListOf<TopLevelDeclarationNode>()
    newDeclarations.addAll(
        this.declarations
            .filterIsInstance<HostDeclarationNode>()
            .map { hostDecl -> hostDecl.deepCopy() as HostDeclarationNode }
    )
    newDeclarations.addAll(newFunctions)
    newDeclarations.add(
        FunctionDeclarationNode(
            main.name,
            main.labelParameters,
            main.parameters,
            main.labelConstraints,
            main.pcLabel,
            newMainBlock,
            main.sourceLocation
        )
    )

    return ProgramNode(newDeclarations, this.sourceLocation)
}

typealias SpecializedFunction = Pair<List<Label>, FunctionName>

private class Specializer(
    val functionMap: Map<FunctionName, FunctionDeclarationNode>,
    val mainProgram: BlockNode
) {
    val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())

    // maintain a worklist of functions to specialize
    val worklist = LinkedList<Triple<FunctionName, FunctionName, CallingContext>>()

    private fun FunctionCallNode.argumentLabels(): List<Label> {

    }

    val informationflowAnalysis : InformationFlowAnalysis

    val monoMap : MutableMap<FunctionDeclarationNode, MutableList<SpecializedFunction>>

    // walk through the program to call this
    fun FunctionCallNode.monomorphize(Map<PolyLabel, Label>) : FunctionCallNode {

    }

    fun specializeStatement(
        callingCtx: CallingContext,
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
                    function.labelParameters,
                    Arguments(
                        function.parameters.map { param -> param.deepCopy() as ParameterNode },
                        function.parameters.sourceLocation
                    ),
                    function.labelConstraints,
                    function.pcLabel,
                    specializeStatement(currentCtx, function.body) as BlockNode,
                    function.sourceLocation
                )
            )
        }

        return Pair(newMain, newFunctions)
    }
}
