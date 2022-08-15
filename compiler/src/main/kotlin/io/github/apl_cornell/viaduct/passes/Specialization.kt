package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.analysis.InformationFlowAnalysis
import io.github.apl_cornell.viaduct.analysis.main
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.LabelParameter
import io.github.apl_cornell.viaduct.security.PolymorphicPrincipal
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionName
import io.github.apl_cornell.viaduct.syntax.HostTrustConfiguration
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


/**
 * A dictionary from (function name and a list of argument labels) to (specialized function name)
 */
class CallingContext(
    private val informationFlowAnalysis: InformationFlowAnalysis,
    functionMap: Map<FunctionName, FunctionDeclarationNode>
) {

    private val hostTrustConfiguration: HostTrustConfiguration = informationFlowAnalysis.trustConfiguration

    // fresh name generator
    private val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())

    // internal storage of the function mapping
    private val context: MutableMap<FunctionName, MutableList<Pair<List<Label>, FunctionName>>> =
        mutableMapOf()

    /**
     * Given a [FunctionCallNode], return a specialized [FunctionName] this callsite refers to
     * Callsites with the same function Names AND the same argument label signatures
     * call the same specialized function.
     */
    fun specializedName(functionCallNode: FunctionCallNode): Pair<FunctionName, Boolean> {
        // name of the function being specialized
        val name: FunctionName = functionCallNode.name.value
        // label arguments of the current callsite
        val argumentLabels = functionCallArgumentLabels(functionCallNode)
        return if (name in context) {
            // the case where the function is already specialized before
            // look for a specialized function that has the same signature
            val targetFunction = context[name]?.find {
                assert(it.first.size == argumentLabels.size)
                it.first.zip(argumentLabels).all { (x, y) -> hostTrustConfiguration.equals(x, y) }
            }
            if (targetFunction != null) {
                // return the specialized function name right away if found
                (targetFunction.second to true)
            } else {
                // if none of the function has the same signature, create new name and put it in list
                val newFunctionName = FunctionName(nameGenerator.getFreshName(name.name))
                context[name]?.add((argumentLabels to newFunctionName))
                (newFunctionName to false)
            }
        } else {
            // the case where function name have not been specialized
            val newFunctionName = FunctionName(nameGenerator.getFreshName(name.name))
            context[name] = mutableListOf((argumentLabels to newFunctionName))
            (newFunctionName to false)
        }
    }

    fun functionCallArgumentLabels(functionCallNode: FunctionCallNode): List<Label> =
        functionCallNode.arguments.map { informationFlowAnalysis.label(it) }
}

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
    program: ProgramNode
) {

    val functionMap: Map<FunctionName, FunctionDeclarationNode> = program.functionMap

    val mainProgram: BlockNode = program.main.body

    val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    val callingContext = CallingContext(informationFlowAnalysis, functionMap)

    val worklist: MutableList<FunctionCallNode> = mutableListOf()

    private fun monomorphizeFunction(
        functionDeclarationNode: FunctionDeclarationNode,
        newFunctionName: FunctionName,
        argumentLabels: List<Label>
    ) {
        // function parameters as LabelExpressions
        val functionParameters = functionDeclarationNode.parameters.map {
            it.objectType.labelArguments!!.first().value
        }
        assert(argumentLabels.size == functionParameters.size)
        // first of each pair is LabelExpression of parameter, second of each is Label of argument
        val parametersToArguments = functionParameters
            // zip parameters with arguments
            .zip(argumentLabels)
            // filter polymorphic arguments (LabelParameters)
            .filter { it.first is LabelParameter }
            // get PolymorphicPrincipals from LabelParameters
            .map { ((PolymorphicPrincipal((it.first as LabelParameter).name) as Principal) to it.second) }
            // break up LabelParameters into Components
            .flatMap {
                listOf(
                    (ConfidentialityComponent(it.first) to it.second.confidentialityComponent),
                    (IntegrityComponent(it.first) to it.second.integrityComponent)
                )
            }
            // make it a map
            .toMap()

        informationFlowAnalysis.monomorphize(
            newFunctionName,
            parametersToArguments
        )
    }


    fun StatementNode.specializeStatement(): StatementNode =
        when (this) {
            is FunctionCallNode -> {
                // get name
                val (specializedName, isNew) = callingContext.specializedName(this)
                // add callsite to worklist if new
                if (isNew) worklist.add(this)
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
                    thenBranch.specializeStatement() as BlockNode,
                    elseBranch.specializeStatement() as BlockNode,
                    sourceLocation
                )
            }

            is InfiniteLoopNode -> {
                InfiniteLoopNode(
                    body.specializeStatement() as BlockNode,
                    jumpLabel,
                    sourceLocation
                )
            }

            is BlockNode -> {
                BlockNode(
                    statements.map { specializeStatement() },
                    sourceLocation
                )
            }

            else -> deepCopy() as StatementNode
        }

    /** Specialize by processing call site in the worklist. */
    fun specialize(): Pair<BlockNode, List<FunctionDeclarationNode>> {
        val newFunctions = mutableListOf<FunctionDeclarationNode>()
        val newMain = mainProgram.specializeStatement() as BlockNode

        while (worklist.isNotEmpty()) {
            // pick an unspecialized callsite
            val callsite = worklist.removeFirst()
            // find newName of the function
            val (newName, isNew) = callingContext.specializedName(callsite)
            val argumentLabels = callingContext.functionCallArgumentLabels(callsite)
            assert(!isNew)
            // find the unspecialized function declaration
            val function = functionMap[callsite.name.value]!!
            // monomorphize the function
            monomorphizeFunction(function, newName, argumentLabels)
            // then specialize
            newFunctions.add(
                FunctionDeclarationNode(
                    Located(newName, function.name.sourceLocation),
                    function.labelParameters,
                    Arguments(
                        function.parameters.map { it.deepCopy() as ParameterNode },
                        function.parameters.sourceLocation
                    ),
                    function.labelConstraints,
                    function.pcLabel,
                    function.body.specializeStatement() as BlockNode,
                    function.sourceLocation
                )
            )
        }

        return Pair(newMain, newFunctions)
    }
}
