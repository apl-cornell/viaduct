package io.github.aplcornell.viaduct.passes

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.analysis.InformationFlowAnalysis
import io.github.aplcornell.viaduct.analysis.main
import io.github.aplcornell.viaduct.security.Component
import io.github.aplcornell.viaduct.security.ConfidentialityComponent
import io.github.aplcornell.viaduct.security.IntegrityComponent
import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.PolymorphicPrincipal
import io.github.aplcornell.viaduct.security.Principal
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.HostTrustConfiguration
import io.github.aplcornell.viaduct.syntax.LabelNode
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclassificationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DelegationDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.EndorsementNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.StatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.TopLevelDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.deepCopy
import io.github.aplcornell.viaduct.util.FreshNameGenerator

typealias PrincipalComponent = Component<Principal>
typealias LabelConstant = FreeDistributiveLattice<PrincipalComponent>

/** Returns an AST where every call site is specialized into new functions as much as possible.
 *  This allows for the most liberal protocol selection possible, at the cost of redundancy.
 *  The specializer will not specialize (mutually) recursive functions to prevent unbounded specialization.
 */
fun ProgramNode.specialize(): ProgramNode {
    val (newMainBlock, newFunctions) = Specializer(this).specialize()

    val newDeclarations = mutableListOf<TopLevelDeclarationNode>()
    newDeclarations.addAll(
        this.declarations
            .filterIsInstance<HostDeclarationNode>()
            .map { hostDecl -> hostDecl.deepCopy() as HostDeclarationNode }
    )
    newDeclarations.addAll(this.declarations.filterIsInstance<DelegationDeclarationNode>())
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

private class Specializer(
    private val program: ProgramNode
) {
    // map from old function name to old function declaration node
    private val functionMap: Map<FunctionName, FunctionDeclarationNode> = program.functionMap

    // old main program
    private val mainProgram: BlockNode = program.main.body

    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    private val hostTrustConfiguration = HostTrustConfiguration.get(program)

    // worklist identified by new functions and corresponding old functionCallNode to be specialized
    private val worklist: MutableList<Triple<FunctionName, FunctionCallNode, Rewrite>> =
        mutableListOf()

    // fresh name generator
    private val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())

    // map from old function name to new function name and signature
    private val context: MutableMap<FunctionName, MutableList<Pair<List<Label>, FunctionName>>> =
        mutableMapOf()

    // map from new function to old function name
    private val reverseContext: MutableMap<FunctionName, Pair<FunctionName, List<Label>>> = mutableMapOf()

    private fun ObjectTypeNode.specialize(rewrites: Rewrite): ObjectTypeNode =
        ObjectTypeNode(
            className.copy(),
            Arguments(
                typeArguments.map { it.copy() },
                typeArguments.sourceLocation
            ),
            if (labelArguments == null) {
                null
            } else {
                Arguments(listOf(labelArguments.first().specialize(rewrites)), labelArguments.sourceLocation)
            }
        )

    private fun LabelNode.specialize(rewrites: Rewrite): LabelNode =
        LabelNode(rewrites.rewrite(value), sourceLocation)

    private fun ExpressionNode.specialize(rewrites: Rewrite): ExpressionNode =
        when (this) {
            is DeclassificationNode ->
                DeclassificationNode(
                    expression.specialize(rewrites) as AtomicExpressionNode,
                    fromLabel?.specialize(rewrites),
                    toLabel.specialize(rewrites),
                    sourceLocation
                )

            is EndorsementNode ->
                EndorsementNode(
                    expression.specialize(rewrites) as AtomicExpressionNode,
                    fromLabel.specialize(rewrites),
                    toLabel?.specialize(rewrites),
                    sourceLocation
                )

            else -> this.copy(this.children.map { it.specialize(rewrites) })
        }

    private fun StatementNode.specialize(rewrites: Rewrite): StatementNode =
        when (this) {
            is FunctionCallNode -> {
// name of the function being specialized
                val name: FunctionName = name.value
// label arguments of the current callsite
                val argumentLabels = arguments.map { rewrites.rewrite(informationFlowAnalysis.label(it)) }
                val specializedName = if (name in context) {
                    // the case where the function is already specialized before
                    // look for a specialized function that has the same signature
                    val targetFunction = context[name]?.find {
                        assert(it.first.size == argumentLabels.size) { "argument label size different from parameter size" }
                        it.first.zip(argumentLabels).all { (x, y) ->
                            hostTrustConfiguration.equals(x, y)
                        }
                    }
                    if (targetFunction != null) {
                        // return the specialized function name right away if found
                        targetFunction.second
                    } else {
                        // if none of the function has the same signature, create new name and put it in list
                        val newFunctionName = FunctionName(nameGenerator.getFreshName(name.name))
                        context[name]?.add((argumentLabels to newFunctionName))
                        reverseContext[newFunctionName] = (name to argumentLabels)
                        worklist.add(Triple(newFunctionName, this, rewrites))
                        newFunctionName
                    }
                } else {
                    // the case where function name have not been specialized
                    val newFunctionName = FunctionName(nameGenerator.getFreshName(name.name))
                    context[name] = mutableListOf((argumentLabels to newFunctionName))
                    reverseContext[newFunctionName] = (name to argumentLabels)
                    worklist.add(Triple(newFunctionName, this, rewrites))
                    newFunctionName
                }
// reconstruct callsite with new name
                FunctionCallNode(
                    Located(specializedName, this.name.sourceLocation),
                    Arguments(
                        arguments.map { arg -> arg.deepCopy() as FunctionArgumentNode },
                        arguments.sourceLocation
                    ),
                    sourceLocation
                )
            }

            is IfNode -> {
                IfNode(
                    guard.deepCopy() as AtomicExpressionNode,
                    thenBranch.specialize(rewrites) as BlockNode,
                    elseBranch.specialize(rewrites) as BlockNode,
                    sourceLocation
                )
            }

            is InfiniteLoopNode -> {
                InfiniteLoopNode(
                    body.specialize(rewrites) as BlockNode,
                    jumpLabel,
                    sourceLocation
                )
            }

            is BlockNode -> {
                BlockNode(
                    statements.map { it.specialize(rewrites) },
                    sourceLocation
                )
            }

            is DeclarationNode ->
                DeclarationNode(
                    name,
                    objectType.specialize(rewrites),
                    Arguments(
                        arguments.map { it.deepCopy() as AtomicExpressionNode },
                        arguments.sourceLocation
                    ),
                    protocol,
                    sourceLocation
                )

            is LetNode ->
                LetNode(
                    name,
                    value.specialize(rewrites),
                    protocol,
                    sourceLocation
                )

            else -> deepCopy() as StatementNode
        }

    private fun FunctionDeclarationNode.specialize(
        rewrites: Rewrite,
        newName: FunctionName
    ): FunctionDeclarationNode =
        FunctionDeclarationNode(
            Located(newName, name.sourceLocation),
            Arguments(labelParameters.sourceLocation),
            Arguments(
                parameters.map {
                    ParameterNode(
                        it.name.copy(),
                        it.parameterDirection,
                        it.objectType.specialize(rewrites),
                        it.protocol,
                        it.sourceLocation
                    )
                },
                parameters.sourceLocation
            ),
            Arguments(labelConstraints.sourceLocation),
            pcLabel.specialize(rewrites),
            body.specialize(rewrites) as BlockNode,
            sourceLocation

        )

    /** Specialize by processing call site in the worklist. */
    fun specialize(): Pair<BlockNode, List<FunctionDeclarationNode>> {
        val newFunctions = mutableListOf<FunctionDeclarationNode>()
        val newMain = mainProgram.specialize(Rewrite(mapOf(), hostTrustConfiguration)) as BlockNode

        while (worklist.isNotEmpty()) {
// pick an unspecialized callsite
            val (newName, oldFunctionCallNode, callsiteRewrite) = worklist.removeFirst()
            val oldName = reverseContext.getOrElse(newName) {
                assert(false) { "reverse context does not find newname" }
                (" " to mutableListOf())
            }.first
// find the unspecialized function declaration
            val oldFunction = functionMap[oldName]!!
            // construct the rewrite map with labels that are already specialized
            val rewrite = Rewrite(
                oldFunction.labelParameters
                    .map {
                        (
                            PolymorphicPrincipal(it.value)
                                to callsiteRewrite.rewrite(
                                    informationFlowAnalysis.label(
                                        oldFunctionCallNode,
                                        it.value
                                    )
                                )
                            )
                    }
// break into components
                    .flatMap {
                        listOf(
                            (ConfidentialityComponent(it.first as Principal) to it.second.confidentialityComponent),
                            (IntegrityComponent(it.first as Principal) to it.second.integrityComponent)
                        )
                    }
// make it a map
                    .toMap(),
                hostTrustConfiguration
            )
// then specialize
// TODO: Want to check label parameters match rewrite keys
            newFunctions.add(oldFunction.specialize(rewrite, newName))
        }

        return Pair(newMain, newFunctions)
    }
}
