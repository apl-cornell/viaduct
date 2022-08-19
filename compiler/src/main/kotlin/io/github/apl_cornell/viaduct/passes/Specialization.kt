package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.analysis.InformationFlowAnalysis
import io.github.apl_cornell.viaduct.analysis.main
import io.github.apl_cornell.viaduct.security.Component
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.HostPrincipal
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.LabelAnd
import io.github.apl_cornell.viaduct.security.LabelBottom
import io.github.apl_cornell.viaduct.security.LabelConfidentiality
import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.security.LabelIntegrity
import io.github.apl_cornell.viaduct.security.LabelJoin
import io.github.apl_cornell.viaduct.security.LabelLiteral
import io.github.apl_cornell.viaduct.security.LabelMeet
import io.github.apl_cornell.viaduct.security.LabelOr
import io.github.apl_cornell.viaduct.security.LabelParameter
import io.github.apl_cornell.viaduct.security.LabelTop
import io.github.apl_cornell.viaduct.security.PolymorphicPrincipal
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.FunctionName
import io.github.apl_cornell.viaduct.syntax.LabelNode
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.ObjectTypeNode
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclassificationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.EndorsementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.TopLevelDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.deepCopy
import io.github.apl_cornell.viaduct.util.FreshNameGenerator

private typealias PrincipalComponent = Component<Principal>
private typealias PolymorphicPrincipalComponent = Component<PolymorphicPrincipal>
private typealias LabelConstant = FreeDistributiveLattice<PrincipalComponent>


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
    // map from old function name to old function declaration node
    private val functionMap: Map<FunctionName, FunctionDeclarationNode> = program.functionMap

    // old main program
    private val mainProgram: BlockNode = program.main.body

    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    // worklist identified by new functions to be specialized
    private val worklist: MutableList<FunctionName> = mutableListOf()

    // fresh name generator
    private val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())

    private val context: MutableMap<FunctionName, MutableList<Pair<List<Label>, FunctionName>>> =
        mutableMapOf()

    // map from new function to old function name
    private val reverseContext: MutableMap<FunctionName, Pair<FunctionName, List<Label>>> = mutableMapOf()

    /**
     * Given a map that maps element to expressions, rewrite by substitution.
     */
    private fun LabelConstant.rewrite(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): LabelConstant =
        joinOfMeets.fold(FreeDistributiveLattice.bounds<PrincipalComponent>().bottom) { accOut, meet ->
            accOut.join(meet.fold(FreeDistributiveLattice.bounds<PrincipalComponent>().top) { accIn, e ->
                accIn.meet(
                    when (e) {
                        is IntegrityComponent -> {
                            when (e.principal) {
                                is HostPrincipal -> FreeDistributiveLattice(e)
                                is PolymorphicPrincipal -> rewrites[e as PolymorphicPrincipalComponent]!!
                            }
                        }

                        is ConfidentialityComponent -> {
                            when (e.principal) {
                                is HostPrincipal -> FreeDistributiveLattice(e)
                                is PolymorphicPrincipal -> rewrites[e as PolymorphicPrincipalComponent]!!
                            }
                        }
                    }
                )
            })
        }

    /**
     * Given a label with polymorphic label and a rewrite map, return a label without polymorphic labels
     */
    private fun Label.rewrite(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): Label =
        Label(confidentialityComponent.rewrite(rewrites), this.integrityComponent.rewrite(rewrites))


    /**
     * Rewrite a PrincipalComponent
     */
    private fun PrincipalComponent.toLabelExpression(): LabelExpression =
        when (this) {
            is IntegrityComponent -> {
                when (principal) {
                    is HostPrincipal -> LabelIntegrity(LabelLiteral(principal.host))
                    is PolymorphicPrincipal -> TODO()
                }
            }

            is ConfidentialityComponent -> {
                when (principal) {
                    is HostPrincipal -> LabelConfidentiality(LabelLiteral(principal.host))
                    is PolymorphicPrincipal -> TODO()
                }
            }
        }

    private fun LabelExpression.rewrite(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): LabelExpression =
        when (this) {
            is LabelParameter ->
                LabelMeet(
                    LabelConfidentiality(rewrites[ConfidentialityComponent(PolymorphicPrincipal(this.name))]!!
                        .joinOfMeets.fold(LabelBottom as LabelExpression) { accOut, meet ->
                            LabelJoin(accOut, meet.fold(LabelTop as LabelExpression) { accIn, e ->
                                LabelMeet(accIn, e.toLabelExpression())
                            })
                        }),
                    LabelIntegrity(rewrites[IntegrityComponent(PolymorphicPrincipal(this.name))]!!
                        .joinOfMeets.fold(LabelBottom as LabelExpression) { accOut, meet ->
                            LabelJoin(accOut, meet.fold(LabelTop as LabelExpression) { accIn, e ->
                                LabelMeet(accIn, e.toLabelExpression())
                            })
                        })
                )

            is LabelAnd -> LabelAnd(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelOr -> LabelOr(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelJoin -> LabelJoin(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelMeet -> LabelMeet(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelConfidentiality -> LabelConfidentiality(value.rewrite(rewrites))
            is LabelIntegrity -> LabelIntegrity(value.rewrite(rewrites))
            else -> this
        }

    private fun LabelNode.specialize(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): LabelNode =
        LabelNode(value.rewrite(rewrites), sourceLocation)

    private fun ObjectTypeNode.specialize(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): ObjectTypeNode =
        ObjectTypeNode(
            className.copy(),
            Arguments(
                typeArguments.map { it.copy() },
                typeArguments.sourceLocation
            ),
            Arguments(listOf(labelArguments?.first()!!.specialize(rewrites)), labelArguments.sourceLocation)
        )


    private fun ExpressionNode.specialize(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): ExpressionNode =
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

            else -> this.deepCopy() as ExpressionNode
        }

    private fun StatementNode.specialize(rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>): StatementNode =
        when (this) {
            is FunctionCallNode -> {
                // name of the function being specialized
                val name: FunctionName = name.value
                // label arguments of the current callsite
                val argumentLabels = arguments.map { informationFlowAnalysis.label(it).rewrite(rewrites) }
                val specializedName = if (name in context) {
                    // the case where the function is already specialized before
                    // look for a specialized function that has the same signature
                    val targetFunction = context[name]?.find {
                        assert(it.first.size == argumentLabels.size)
                        it.first.zip(argumentLabels).all { (x, y) ->
                            informationFlowAnalysis.trustConfiguration.equals(x, y)
                        }
                    }
                    if (targetFunction != null) {
                        worklist.add(targetFunction.second)
                        // return the specialized function name right away if found
                        targetFunction.second
                    } else {
                        // if none of the function has the same signature, create new name and put it in list
                        val newFunctionName = FunctionName(nameGenerator.getFreshName(name.name))
                        context[name]?.add((argumentLabels to newFunctionName))
                        reverseContext[newFunctionName] = (name to argumentLabels)
                        newFunctionName
                    }
                } else {
                    // the case where function name have not been specialized
                    val newFunctionName = FunctionName(nameGenerator.getFreshName(name.name))
                    context[name] = mutableListOf((argumentLabels to newFunctionName))
                    reverseContext[newFunctionName] = (name to argumentLabels)
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
        rewrites: Map<PolymorphicPrincipalComponent, LabelConstant>,
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
            pcLabel?.specialize(rewrites),
            body.specialize(rewrites) as BlockNode,
            sourceLocation
        )

    /** Specialize by processing call site in the worklist. */
    fun specialize(): Pair<BlockNode, List<FunctionDeclarationNode>> {
        val newFunctions = mutableListOf<FunctionDeclarationNode>()
        val newMain = mainProgram.specialize(mapOf()) as BlockNode

        while (worklist.isNotEmpty()) {
            // pick an unspecialized callsite
            val newName = worklist.removeFirst()
            val (oldName, argumentLabels) = reverseContext.getOrElse(newName) {
                assert(false)
                (" " to mutableListOf())
            }
            // find the unspecialized function declaration
            val oldFunction = functionMap[oldName]!!
            // create rewrite map for from PrincipalComponent to Label
            val rewriteMap = oldFunction.parameters
                // function parameters as LabelExpressions
                .map {
                    it.objectType.labelArguments!!.first().value
                }
                // zip parameters with arguments
                .zip(argumentLabels)
                // filter polymorphic arguments (LabelParameters)
                .filter { it.first is LabelParameter }
                // get PolymorphicPrincipals from LabelParameters
                .map { (PolymorphicPrincipal((it.first as LabelParameter).name) to it.second) }
                // break into components
                .flatMap {
                    listOf(
                        (ConfidentialityComponent(it.first) to it.second.confidentialityComponent),
                        (IntegrityComponent(it.first) to it.second.integrityComponent)
                    )
                }
                // make it a map
                .toMap()
            // then specialize
            // TODO: Want to check label parameters match rewrite keys
            newFunctions.add(oldFunction.specialize(rewriteMap, newName))
        }

        return Pair(newMain, newFunctions)
    }
}
