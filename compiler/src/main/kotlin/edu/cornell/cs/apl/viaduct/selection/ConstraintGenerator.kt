package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.analysis.updateNodes
import edu.cornell.cs.apl.viaduct.errors.UnknownObjectDeclarationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclaration
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.unions
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

    fun viableProtocols(node: ObjectDeclarationArgumentNode): Set<Protocol> =
        protocolFactory.viableProtocols(node).filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

    /** Generate constraints for possible protocols. */
    private fun Node.selectionConstraints(): Set<SelectionConstraint> =
        when (this) {
            is LetNode ->
                setOf(protocolFactory.constraint(this)).plus(
                    // extra constraints
                    when (val rhs = this.value) {
                        is InputNode -> {
                            setOf(
                                VariableIn(
                                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                                    setOf(Local(rhs.host.value))
                                )
                            )
                        }

                        // queries needs to be executed in the same protocol as the object
                        is QueryNode -> {
                            val enclosingFunctionName = nameAnalysis.enclosingFunctionName(this)

                            setOf(
                                VariableIn(
                                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                                    viableProtocols(this)
                                )
                            ).plus(
                                when (val objectDecl = nameAnalysis.declaration(rhs).declarationAsNode) {
                                    is DeclarationNode ->
                                        VariableEquals(
                                            FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                            FunctionVariable(enclosingFunctionName, this.temporary.value)
                                        )

                                    is ParameterNode ->
                                        VariableEquals(
                                            FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                            FunctionVariable(enclosingFunctionName, this.temporary.value)
                                        )

                                    is ObjectDeclarationArgumentNode -> {
                                        val param = nameAnalysis.parameter(objectDecl)
                                        VariableEquals(
                                            FunctionVariable(
                                                nameAnalysis.functionDeclaration(param).name.value,
                                                param.name.value
                                            ),
                                            FunctionVariable(enclosingFunctionName, this.temporary.value)
                                        )
                                    }

                                    else -> throw UnknownObjectDeclarationError(objectDecl)
                                }
                            )
                        }

                        else -> {
                            setOf(
                                VariableIn(
                                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                                    viableProtocols(this)
                                )
                            )
                        }
                    }
                )

            is DeclarationNode ->
                setOf(
                    VariableIn(
                        FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                        viableProtocols(this)
                    ),
                    protocolFactory.constraint(this)
                )

            // generate constraints for the if node
            // used by the ABY/MPC factory to generate muxing constraints
            is IfNode -> setOf(protocolFactory.constraint(this))

            is ObjectDeclarationArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                        FunctionVariable(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            // argument protocol must equal the parameter protocol
            is ExpressionArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                nameAnalysis
                    .reads(this)
                    .map { read -> nameAnalysis.declaration(read) }
                    .map { letNode ->
                        VariableEquals(
                            FunctionVariable(nameAnalysis.enclosingFunctionName(letNode), letNode.temporary.value),
                            FunctionVariable(parameterFunctionName, parameter.name.value)
                        )
                    }
                    .toSet()
            }

            // argument protocol must equal the parameter protocol
            is ObjectReferenceArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.variable.value),
                        FunctionVariable(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            // argument protocol must equal the parameter protocol
            is OutParameterArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.parameter.value),
                        FunctionVariable(parameterFunctionName, parameter.name.value)
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

    private val zeroSymbolicCost = costEstimator.zeroCost().map { CostLiteral(0) }

    /** Symbolic cost associated with a node. */
    private val Node.symbolicCost: Cost<SymbolicCost> by attribute {
        when (this) {
            is FunctionDeclarationNode -> this.body.symbolicCost

            is ProcessDeclarationNode -> this.body.symbolicCost

            is BlockNode ->
                this.statements
                    .fold(zeroSymbolicCost) { acc, childStmt ->
                        acc.concat(childStmt.symbolicCost)
                    }

            is IfNode ->
                this.guard.symbolicCost
                    .concat(this.thenBranch.symbolicCost)
                    .concat(this.elseBranch.symbolicCost)

            is InfiniteLoopNode ->
                this.body.symbolicCost.map { f -> CostMul(CostLiteral(10), f) }

            // TODO: handle this later, recursive functions are tricky
            is FunctionCallNode -> zeroSymbolicCost

            is BreakNode -> zeroSymbolicCost

            is AssertionNode -> zeroSymbolicCost

            else ->
                costEstimator
                    .zeroCost()
                    .map { CostVariable(ctx.mkFreshConst("cost", ctx.intSort) as IntExpr) }
        }
    }

    fun symbolicCost(node: Node) = node.symbolicCost

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
                        VariableIn(FunctionVariable(fv.function, kv.key), setOf(kv.value))
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

    private fun getObjectDeclarationViableProtocols(
        decl: ObjectDeclaration
    ): Pair<FunctionVariable, Set<Protocol>> =
        when (val node = decl.declarationAsNode) {
            is DeclarationNode ->
                Pair(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(node), node.name.value),
                    viableProtocols(node)
                )

            is ParameterNode ->
                Pair(
                    FunctionVariable(
                        nameAnalysis.functionDeclaration(node).name.value,
                        node.name.value
                    ),
                    viableProtocols(node)
                )

            is ObjectDeclarationArgumentNode -> {
                val param = nameAnalysis.parameter(node)
                Pair(
                    FunctionVariable(
                        nameAnalysis.functionDeclaration(param).name.value,
                        node.name.value
                    ),
                    viableProtocols(param)
                )
            }

            else -> throw UnknownObjectDeclarationError(node)
        }

    /** Generate cost constraints. */
    private fun Node.costConstraints(): Set<SelectionConstraint> =
        when (this) {
            // induce execution and communication costs
            is LetNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                    viableProtocols(this),
                    nameAnalysis.reads(this.value).toList(),
                    { protocol -> costEstimator.executionCost(this.value, protocol) },
                    this.symbolicCost
                )
            }

            // induce storage and communication costs
            is DeclarationNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                    viableProtocols(this),
                    this.arguments.filterIsInstance<ReadNode>(),
                    { protocol -> costEstimator.storageCost(this, protocol) },
                    this.symbolicCost
                )
            }

            // induce execution and communication costs
            is UpdateNode -> {
                val (fv, protocols) = getObjectDeclarationViableProtocols(nameAnalysis.declaration(this))

                generateComputationCostConstraints(
                    fv,
                    protocols,
                    this.arguments.filterIsInstance<ReadNode>(),
                    { protocol ->
                        val decl = nameAnalysis.declaration(this)
                        costEstimator.storageCost(decl, protocol)
                    },
                    this.symbolicCost
                )
            }

            // storage and communication cost for initializing an out parameter
            is OutParameterInitializationNode -> {
                val parameter = nameAnalysis.declaration(this)
                val reads: List<ReadNode> =
                    when (val initializer = this.initializer) {
                        is OutParameterConstructorInitializerNode ->
                            initializer.arguments.filterIsInstance<ReadNode>()

                        is OutParameterExpressionInitializerNode ->
                            if (initializer.expression is ReadNode) listOf(initializer.expression) else listOf()
                    }

                generateComputationCostConstraints(
                    FunctionVariable(
                        nameAnalysis.functionDeclaration(parameter).name.value,
                        parameter.name.value
                    ),
                    viableProtocols(parameter),
                    reads,
                    { protocol -> costEstimator.storageCost(parameter, protocol) },
                    this.symbolicCost
                )
            }

            // generate cost constraints for when temporaries are read as guards
            // induce communication cost from guard protocol to all protocols
            // participating in the conditional
            is IfNode -> {
                when (val guard = this.guard) {
                    is LiteralNode ->
                        setOf(
                            symbolicCostEqualsInt(this.guard.symbolicCost, costEstimator.zeroCost())
                        )

                    is ReadNode -> {
                        val guardDecl = nameAnalysis.declaration(guard)
                        val guardProtocols = viableProtocols(guardDecl)
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(this)

                        // map from declaration/let nodes to viable protocols
                        val viableProtocolMap: Map<FunctionVariable, Set<Protocol>> =
                            this.letNodes().map { letNode ->
                                Pair(
                                    FunctionVariable(enclosingFunctionName, letNode.temporary.value),
                                    viableProtocols(letNode)
                                )
                            }.plus(
                                this.declarationNodes().map { decl ->
                                    getObjectDeclarationViableProtocols(decl)
                                }
                            ).plus(
                                this.updateNodes().map { update ->
                                    getObjectDeclarationViableProtocols(nameAnalysis.declaration(update))
                                }
                            ).toMap()

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
                                            if (cost.cost != 0) {
                                                CostMux(
                                                    kv.value.fold(
                                                        Literal(false) as SelectionConstraint
                                                    ) { acc2, fv ->
                                                        Or(acc2, VariableIn(fv, setOf(kv.key)))
                                                    },
                                                    CostLiteral(cost.cost),
                                                    CostLiteral(0)
                                                )
                                            } else {
                                                CostLiteral(0)
                                            }
                                        }
                                    )
                                }

                            Implies(
                                VariableIn(
                                    FunctionVariable(enclosingFunctionName, guardDecl.temporary.value),
                                    setOf(guardProtocol)
                                ),
                                symbolicCostEqualsSym(this.guard.symbolicCost, protocolCost)
                            )
                        }.toSet()
                    }
                }
            }

            // induce communication costs from message protocol to output protocol
            is OutputNode -> {
                when (val msg = this.message) {
                    is LiteralNode ->
                        setOf(
                            symbolicCostEqualsInt(this.symbolicCost, costEstimator.zeroCost())
                        )

                    is ReadNode -> {
                        val msgDecl = nameAnalysis.declaration(msg)
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(msgDecl)
                        val msgProtocols = viableProtocols(msgDecl)
                        val outputProtocol = Local(this.host.value)

                        msgProtocols.map { msgProtocol ->
                            Implies(
                                VariableIn(
                                    FunctionVariable(enclosingFunctionName, msgDecl.temporary.value),
                                    setOf(msgProtocol)
                                ),
                                symbolicCostEqualsInt(
                                    this.symbolicCost,
                                    costEstimator.communicationCost(msgProtocol, outputProtocol)
                                )
                            )
                        }.toSet()
                    }
                }
            }

            else -> setOf()
        }

    /** Generate both selection and cost constraints. */
    private fun Node.constraints(): Set<SelectionConstraint> =
        this.selectionConstraints()
            .union(this.costConstraints())
            .union(this.children.map { it.constraints() }.unions())

    fun getConstraints(node: Node) = node.constraints()

    fun getSelectionConstraints(node: Node): Set<SelectionConstraint> =
        node.selectionConstraints().union(node.children.map { it.constraints() }.unions())
}
