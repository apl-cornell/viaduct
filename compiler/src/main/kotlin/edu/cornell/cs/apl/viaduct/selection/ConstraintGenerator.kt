package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.errors.UnknownObjectDeclarationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

class ConstraintGenerator(
    private val program: ProgramNode,
    private val protocolFactory: ProtocolFactory,
    private val costEstimator: CostEstimator<IntegerCost>,
    private val ctx: Context
) {
    private val hostTrustConfiguration = HostTrustConfiguration(program)
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    // TODO: pc must be weak enough for the hosts involved in the selected protocols to read it
    fun viableProtocols(node: LetNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun viableProtocols(node: DeclarationNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun viableProtocols(node: ParameterNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    fun selectionConstraints(node: Node): Set<SelectionConstraint> =
        when (node) {
            is LetNode ->
                setOf(
                    VariableIn(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                        viableProtocols(node)
                    ),
                    protocolFactory.constraint(node)
                ) + when (node.value) {
                    is InputNode ->
                        setOf(
                            VariableIn(
                                Pair(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                                setOf(Local(node.value.host.value))
                            )
                        )
                    is QueryNode -> {
                        val decl = nameAnalysis.declaration(node.value)
                        val declFn: FunctionName = when (val n = decl.declarationAsNode) {
                            is DeclarationNode -> nameAnalysis.enclosingFunctionName(n)

                            is ParameterNode -> nameAnalysis.functionDeclaration(n).name.value

                            is ObjectDeclarationArgumentNode -> nameAnalysis.enclosingFunctionName(n)

                            else -> throw Exception("impossible")
                        }

                        setOf(
                            VariableEquals(
                                Pair(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                                Pair(declFn, decl.name.value)
                            )
                        )
                    }
                    else -> setOf()
                }

            is DeclarationNode ->
                setOf(
                    VariableIn(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.name.value),
                        viableProtocols(node)
                    ),
                    protocolFactory.constraint(node)
                )

            // generate constraints for the if node
            // used by the ABY/MPC factory to generate muxing constraints
            is IfNode -> setOf(protocolFactory.constraint(node))

            is ExpressionArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                nameAnalysis
                    .reads(node)
                    .map { read -> nameAnalysis.declaration(read) }
                    .map { letNode ->
                        VariableEquals(
                            Pair(nameAnalysis.enclosingFunctionName(letNode), letNode.temporary.value),
                            Pair(parameterFunctionName, parameter.name.value)
                        )
                    }
                    .toSet()
            }

            is ObjectReferenceArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.variable.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            is OutParameterArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.parameter.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            is ObjectDeclarationArgumentNode -> {
                val parameter = nameAnalysis.parameter(node)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(node), node.name.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            else -> setOf()
        }

    /**
     * Computes the cartesian product of viable protocols for arguments.
     *
     * @param previous the map of viable protocols selected in this path
     * @param next the remaining arguments to process
     * @return the cartesian product of maps from temporaries to viable protocols
     */
    private fun getArgumentViableProtocols(
        previous: PersistentMap<Temporary, Protocol>,
        next: List<ReadNode>
    ): Set<PersistentMap<Temporary, Protocol>> {
        return if (next.isEmpty()) {
            setOf(previous)
        } else {
            val current = next.first()
            val tail = next.subList(1, next.size)
            val protocols = viableProtocols(nameAnalysis.declaration(current))
            protocols.flatMap { protocol ->
                getArgumentViableProtocols(
                    previous.put(current.temporary.value, protocol),
                    tail
                )
            }.toSet()
        }
    }

    /**
     * Generate constraints that set two symbolic costs equal to each other.
     *
     * @param symCost: symbolic cost
     * @param symCost2: integer cost
     * @return selection constraints that set the features of [symCost] and [symCost2] equal.
     */
    private fun symbolicCostEqualsSym(symCost: Cost<SymbolicCost>, symCost2: Cost<SymbolicCost>): SelectionConstraint =
        symCost.features.map { kv ->
            CostEquals(
                kv.value,
                symCost2.features[kv.key]
                    ?: throw Error("No cost associated with feature ${kv.key}")
            )
        }.fold(Literal(true) as SelectionConstraint) { acc, c -> And(acc, c) }

    /**
     * Generate constraints that set a symbolic cost equal to some integer cost.
     *
     * @param symCost: symbolic cost
     * @param intCost: integer cost
     * @return selection constraints that set the features of [symCost] and [intCost] equal.
     */
    private fun symbolicCostEqualsInt(symCost: Cost<SymbolicCost>, intCost: Cost<IntegerCost>): SelectionConstraint =
        symbolicCostEqualsSym(symCost, intCost.map { c -> CostLiteral(c.cost) })

    /**
     * Generate cost constraints for performing a computation (let nodes and updates).
     * Handles computation of communication costs.
     *
     * @param fv: function-variable pair associated with the computation
     * @param protocols: protocols that can implement the computation
     * @param reads: the reads performed by the computation
     * @param baseCostFunction: basic cost of computation node (no communication cost) as a function of its protocol
     * @param symbolicCost: symbolic cost associated with the computation node.
     * */
    private fun generateComputationCostConstraints(
        fv: FunctionVariable,
        protocols: Set<Protocol>,
        reads: List<ReadNode>,
        baseCostFunction: (Protocol) -> Cost<IntegerCost>,
        symbolicCost: Cost<SymbolicCost>
    ): Set<SelectionConstraint> {
        // cartesian product of all viable protocols for arguments
        val argProtocolMaps =
            getArgumentViableProtocols(persistentMapOf(), reads)

        // for each element in the cartesian product of viable protocols,
        // compute the cost using the cost estimator
        return protocols.flatMap { protocol ->
            argProtocolMaps.map { argProtocolMap ->
                val protocolConstraints: SelectionConstraint =
                    argProtocolMap.map { kv ->
                        VariableIn(FunctionVariable(fv.first, kv.key), setOf(kv.value))
                    }.plus(
                        setOf(VariableIn(fv, setOf(protocol)))
                    ).fold(Literal(true) as SelectionConstraint) { acc, c -> And(acc, c) }

                // estimate cost given a particular configuration of an executing protocol
                // and protocols for arguments
                val cost =
                    baseCostFunction(protocol).concat(
                        argProtocolMap.values.fold(costEstimator.zeroCost()) { acc, argProtocol ->
                            acc.concat(costEstimator.communicationCost(argProtocol, protocol))
                        }
                    )

                Implies(
                    protocolConstraints,
                    symbolicCostEqualsInt(symbolicCost, cost)
                )
            }
        }.toSet()
    }

    private val Node.symbolicCost: Cost<SymbolicCost> by attribute {
        costEstimator
            .zeroCost()
            .map { CostVariable(ctx.mkFreshConst("cost", ctx.intSort) as IntExpr) }
    }

    fun symbolicCost(node: Node) = node.symbolicCost

    /** Generate cost constraints. */
    fun costConstraints(node: Node): Set<SelectionConstraint> =
        when (node) {
            // induce execution and communication costs
            is LetNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(node), node.temporary.value),
                    viableProtocols(node),
                    nameAnalysis.reads(node.value).toList(),
                    { protocol -> costEstimator.executionCost(node.value, protocol) },
                    node.symbolicCost
                )
            }

            // induce storage and communication costs
            is DeclarationNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(node), node.name.value),
                    viableProtocols(node),
                    node.arguments.filterIsInstance<ReadNode>(),
                    { protocol -> costEstimator.storageCost(node, protocol) },
                    node.symbolicCost
                )
            }

            // induce execution and communication costs
            is UpdateNode -> {
                val (fv, protocols) =
                    when (val objectDecl = nameAnalysis.declaration(node).declarationAsNode) {
                        is DeclarationNode ->
                            Pair(
                                FunctionVariable(nameAnalysis.enclosingFunctionName(objectDecl), objectDecl.name.value),
                                viableProtocols(objectDecl)
                            )

                        is ParameterNode ->
                            Pair(
                                FunctionVariable(
                                    nameAnalysis.functionDeclaration(objectDecl).name.value,
                                    objectDecl.name.value
                                ),
                                viableProtocols(objectDecl)
                            )

                        is ObjectDeclarationArgumentNode -> {
                            val param = nameAnalysis.parameter(objectDecl)
                            Pair(
                                FunctionVariable(
                                    nameAnalysis.functionDeclaration(param).name.value,
                                    objectDecl.name.value
                                ),
                                viableProtocols(param)
                            )
                        }

                        else -> throw UnknownObjectDeclarationError(objectDecl)
                    }

                generateComputationCostConstraints(
                    fv,
                    protocols,
                    node.arguments.filterIsInstance<ReadNode>(),

                    { protocol ->
                        val decl = nameAnalysis.declaration(node)
                        costEstimator.storageCost(decl, protocol)
                    },
                    node.symbolicCost
                )
            }

            // induce costs for storing the parameter
            is ParameterNode -> {
                val enclosingFunctionName =
                    nameAnalysis.functionDeclaration(node).name.value

                viableProtocols(node).map { protocol ->
                    val cost = costEstimator.storageCost(node, protocol)
                    Implies(
                        VariableIn(
                            FunctionVariable(enclosingFunctionName, node.name.value),
                            setOf(protocol)
                        ),
                        symbolicCostEqualsInt(node.symbolicCost, cost)
                    )
                }.toSet()
            }

            // generate cost constraints for when temporaries are read as guards
            // induce communication cost from guard protocol to all protocols
            // participating in the conditional
            is IfNode -> {
                when (val guard = node.guard) {
                    is LiteralNode ->
                        setOf(
                            symbolicCostEqualsInt(node.symbolicCost, costEstimator.zeroCost())
                        )

                    is ReadNode -> {
                        val guardDecl = nameAnalysis.declaration(guard)
                        val guardProtocols = viableProtocols(guardDecl)
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(node)

                        // map from declaration/let nodes to viable protocols
                        val viableProtocolMap: Map<FunctionVariable, Set<Protocol>> =
                            node.letNodes().map { letNode ->
                                Pair(
                                    FunctionVariable(enclosingFunctionName, letNode.temporary.value),
                                    viableProtocols(letNode)
                                )
                            }.union(
                                node.declarationNodes().map { decl ->
                                    Pair(
                                        FunctionVariable(enclosingFunctionName, decl.name.value),
                                        viableProtocols(decl)
                                    )
                                }
                            )
                                .toMap()

                        // create inverse map from protocols to nodes that can potentially
                        // have it as its primary protocol
                        val participatingProtocolMap: MutableMap<Protocol, MutableSet<FunctionVariable>> =
                            mutableMapOf()
                        for (kv in viableProtocolMap) {
                            for (protocol in kv.value) {
                                if (participatingProtocolMap.containsKey(protocol)) {
                                    participatingProtocolMap[protocol]!!.add(kv.key)
                                } else {
                                    participatingProtocolMap[protocol] = mutableSetOf(kv.key)
                                }
                            }
                        }

                        guardProtocols.map { guardProtocol ->
                            val protocolCost =
                                participatingProtocolMap.entries.fold(
                                    costEstimator.zeroCost().map { CostLiteral(0) }
                                ) { acc, kv ->
                                    acc.concat(
                                        // induce communication cost from guard protocol to participating protocol
                                        // when at least one of the declaration/let nodes actually has it as a primary protocol
                                        // otherwise, the cost is zero
                                        // we model this using a mux expression that maps over all cost features
                                        costEstimator.communicationCost(guardProtocol, kv.key).map { cost ->
                                            CostMux(
                                                kv.value.fold(
                                                    Literal(false) as SelectionConstraint
                                                ) { acc2, fv ->
                                                    Or(acc2, VariableIn(fv, setOf(kv.key)))
                                                },
                                                CostLiteral(cost.cost),
                                                CostLiteral(0)
                                            )
                                        }
                                    )
                                }

                            Implies(
                                VariableIn(
                                    FunctionVariable(enclosingFunctionName, guardDecl.temporary.value),
                                    setOf(guardProtocol)
                                ),
                                symbolicCostEqualsSym(node.symbolicCost, protocolCost)
                            )
                        }.toSet()
                    }
                }
            }

            // induce communication costs from message protocol to output protocol
            is OutputNode -> {
                when (val msg = node.message) {
                    is LiteralNode ->
                        setOf(
                            symbolicCostEqualsInt(node.symbolicCost, costEstimator.zeroCost())
                        )

                    is ReadNode -> {
                        val msgDecl = nameAnalysis.declaration(msg)
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(msgDecl)
                        val msgProtocols = viableProtocols(msgDecl)
                        val outputProtocol = Local(node.host.value)

                        msgProtocols.map { msgProtocol ->
                            Implies(
                                VariableIn(
                                    FunctionVariable(enclosingFunctionName, msgDecl.temporary.value),
                                    setOf(msgProtocol)
                                ),
                                symbolicCostEqualsInt(
                                    node.symbolicCost,
                                    costEstimator.communicationCost(msgProtocol, outputProtocol)
                                )
                            )
                        }.toSet()
                    }
                }
            }

            else -> setOf()
        }
}
