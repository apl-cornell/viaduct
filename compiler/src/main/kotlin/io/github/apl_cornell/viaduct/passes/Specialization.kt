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

    // worklist identified by new functions and corresponding old functionCallNode to be specialized
    private val worklist: MutableList<Triple<FunctionName, FunctionCallNode, Map<PrincipalComponent, LabelConstant>>> =
        mutableListOf()

    // fresh name generator
    private val nameGenerator = FreshNameGenerator(functionMap.keys.map { f -> f.name }.toSet())

    // map from old function name to new function name and signature
    private val context: MutableMap<FunctionName, MutableList<Pair<List<Label>, FunctionName>>> =
        mutableMapOf()

    // map from new function to old function name
    private val reverseContext: MutableMap<FunctionName, Pair<FunctionName, List<Label>>> = mutableMapOf()


    /**
     * Given a map that maps element to expressions, rewrite by substitution.
     */
    private fun LabelConstant.rewrite(rewrites: Map<PrincipalComponent, LabelConstant>): LabelConstant =
        joinOfMeets.fold(FreeDistributiveLattice.bounds<PrincipalComponent>().bottom) { accOut, meet ->
            accOut.join(
                meet.fold(FreeDistributiveLattice.bounds<PrincipalComponent>().top) { accIn, e ->
                    accIn.meet(
                        when (e.principal) {
                            is HostPrincipal -> FreeDistributiveLattice(e)
                            is PolymorphicPrincipal -> rewrites[e]!!
                        }
                    )
                }
            )
        }

    /**
     * Given a label with polymorphic label and a rewrite map, return a label without polymorphic labels
     */
    private fun Label.rewrite(rewrites: Map<PrincipalComponent, LabelConstant>): Label =
        Label(confidentialityComponent.rewrite(rewrites), this.integrityComponent.rewrite(rewrites))

    private fun LabelExpression.rewrite(rewrites: Map<PrincipalComponent, LabelConstant>): LabelExpression =
        when (this) {
            is LabelParameter -> {
                /*val confidentialityRewrite = rewrites[ConfidentialityComponent(PolymorphicPrincipal(this.name))]!!
                val integrityRewrite = rewrites[ConfidentialityComponent(PolymorphicPrincipal(this.name))]!!

                LabelMeet(
                    LabelConfidentiality(
                        confidentialityRewrite.joinOfMeets.fold(LabelBottom as LabelExpression) { accOut, meet ->
                            LabelJoin(
                                accOut,
                                meet.fold(LabelTop as LabelExpression) { accIn, e ->
                                    LabelMeet(accIn, LabelLiteral((e.principal as HostPrincipal).host))
                                }
                            )
                        }
                    ),
                    LabelIntegrity(
                            integrityRewrite.joinOfMeets.fold(LabelBottom as LabelExpression) { accOut, meet ->
                                LabelJoin(
                                    accOut,
                                    meet.fold(LabelTop as LabelExpression) { accIn, e ->
                                        LabelMeet(accIn, LabelLiteral((e.principal as HostPrincipal).host))
                                    }
                                )
                            }
                    )
                )*/

                val confidentialityRewrite =
                    LabelConfidentiality(rewrites[ConfidentialityComponent(PolymorphicPrincipal(this.name))]!!.joinOfMeets
                        .map { meet ->
                            meet.map { LabelLiteral((it.principal as HostPrincipal).host) }
                                .reduceOrNull<LabelExpression, LabelExpression> { acc, e -> LabelMeet(acc, e) }
                                ?: LabelBottom
                        }
                        .reduceOrNull { acc, e -> LabelJoin(acc, e) } ?: LabelTop)

                val integrityRewrite =
                    LabelIntegrity(rewrites[IntegrityComponent(PolymorphicPrincipal(this.name))]!!.joinOfMeets
                        .map { meet ->
                            meet
                                .map { LabelLiteral((it.principal as HostPrincipal).host) }
                                .reduceOrNull<LabelExpression, LabelExpression> { acc, e -> LabelMeet(acc, e) }
                                ?: LabelBottom
                        }
                        .reduceOrNull { acc, e -> LabelJoin(acc, e) } ?: LabelTop)

                LabelMeet(confidentialityRewrite, integrityRewrite)
            }

            is LabelAnd -> LabelAnd(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelOr -> LabelOr(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelJoin -> LabelJoin(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelMeet -> LabelMeet(lhs.rewrite(rewrites), rhs.rewrite(rewrites))
            is LabelConfidentiality -> LabelConfidentiality(value.rewrite(rewrites))
            is LabelIntegrity -> LabelIntegrity(value.rewrite(rewrites))
            else -> this
        }

    private fun LabelNode.specialize(rewrites: Map<PrincipalComponent, LabelConstant>): LabelNode =
        LabelNode(value.rewrite(rewrites), sourceLocation)

    private fun ObjectTypeNode.specialize(rewrites: Map<PrincipalComponent, LabelConstant>): ObjectTypeNode =
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

    private fun ExpressionNode.specialize(rewrites: Map<PrincipalComponent, LabelConstant>): ExpressionNode =
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

    private fun StatementNode.specialize(rewrites: Map<PrincipalComponent, LabelConstant>): StatementNode =
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
                        assert(it.first.size == argumentLabels.size) { "argument label size different from parameter size" }
                        it.first.zip(argumentLabels).all { (x, y) ->
                            informationFlowAnalysis.trustConfiguration.equals(x, y)
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
        rewrites: Map<PrincipalComponent, LabelConstant>,
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
            val (newName, oldFunctionCallNode, callsiteRewrite) = worklist.removeFirst()
            val oldName = reverseContext.getOrElse(newName) {
                assert(false) { "reverse context does not find newname" }
                (" " to mutableListOf())
            }.first
// find the unspecialized function declaration
            val oldFunction = functionMap[oldName]!!
            // construct the rewrite map with labels that are already specialized
            val rewrite = oldFunction.labelParameters
                .map {
                    (PolymorphicPrincipal(it.value)
                        to informationFlowAnalysis.label(
                        oldFunctionCallNode,
                        it.value
                    ).rewrite(callsiteRewrite))
                }
// break into components
                .flatMap {
                    listOf(
                        (ConfidentialityComponent(it.first as Principal) to it.second.confidentialityComponent),
                        (IntegrityComponent(it.first as Principal) to it.second.integrityComponent)
                    )
                }
// make it a map
                .toMap()
// then specialize
// TODO: Want to check label parameters match rewrite keys
            newFunctions.add(oldFunction.specialize(rewrite, newName))
        }

        return Pair(newMain, newFunctions)
    }
}
