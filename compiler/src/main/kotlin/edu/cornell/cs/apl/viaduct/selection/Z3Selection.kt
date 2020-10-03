package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.toBiMap
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.errors.NoProtocolIndexMapping
import edu.cornell.cs.apl.viaduct.errors.NoSelectionSolutionError
import edu.cornell.cs.apl.viaduct.errors.NoVariableSelectionSolutionError
import edu.cornell.cs.apl.viaduct.errors.UnknownObjectDeclarationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.unions
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * This class performs splitting by using Z3. It operates as follows:
 * - First, it collects constraints on protocol selection from the [ProtocolFactory]. For each let or declaration,
 *      the factory outputs two things: first, it outputs a set of viable protocols for that variable. Second,
 *      it can output a number of custom constraints on selection for that variable which are forwarded to Z3.
 *      (For the simple factory, the custom constraints are trivial, as we have not yet constrained which protocols
 *      can talk to who.)
 * - Second, it exports these constraints to Z3. The selection problem is encoded as follows:
 *      - We assign each possible viable protocol a unique integer index. Call this index i(p).
 *      - For each variable, we create a fresh integer constant. Call this constant c(v).
 *      - For each variable v with viable protocols P, we constrain that c(v) is contained in the image set of P under i.
 *      - For each variable v, we constrain c(v) relative to the custom constraints output by the factory.
 * - Third, we ask Z3 to optimize relative to a cost metric. For now, the cost metric is the sum of costs of all protocols
 *       selected. This is particularly naive, as it regards queries/declassifies as having a cost, even though it is
 *       likely free in all backends.
 * - Finally, we ask Z3 for a model, which we may convert into a function of type Variable -> Protocol.
 *
 */
private class Z3Selection(
    private val program: ProgramNode,
    private val main: ProcessDeclarationNode,
    private val protocolFactory: ProtocolFactory,
    private val costEstimator: CostEstimator<IntegerCost>,
    private val ctx: Context
) {
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)
    private val hostTrustConfiguration = HostTrustConfiguration(program)

    init {
        if (this.hostTrustConfiguration.isEmpty()) {
            throw NoHostDeclarationsError(program.sourceLocation.sourcePath)
        }
    }

    // TODO: pc must be weak enough for the hosts involved in the selected protocols to read it
    private val protocolSelection = object {
        private val LetNode.viableProtocols: Set<Protocol> by attribute {
            when (value) {
                is InputNode ->
                    setOf(Local(value.host.value))

                is ReceiveNode -> throw IllegalInternalCommunicationError(value)

                else -> {
                    val label = informationFlowAnalysis.label(this)
                    protocolFactory.viableProtocols(this).filter {
                        it.authority(hostTrustConfiguration).actsFor(label)
                    }.toSet()
                }
            }
        }

        private val DeclarationNode.viableProtocols: Set<Protocol> by attribute {
            protocolFactory.viableProtocols(this)
        }

        private val ParameterNode.viableProtocols: Set<Protocol> by attribute {
            protocolFactory.viableProtocols(this)
        }

        fun viableProtocols(node: LetNode): Set<Protocol> = node.viableProtocols

        fun viableProtocols(node: DeclarationNode): Set<Protocol> = node.viableProtocols

        fun viableProtocols(node: ParameterNode): Set<Protocol> = node.viableProtocols
    }

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
                                    protocolSelection.viableProtocols(this)
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
                                    protocolSelection.viableProtocols(this)
                                )
                            )
                        }
                    }
                )

            is DeclarationNode ->
                setOf(
                    VariableIn(
                        FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                        protocolSelection.viableProtocols(this)
                    ),
                    protocolFactory.constraint(this)
                )

            // generate constraints for the if node
            // used by the ABY/MPC factory to generate muxing constraints
            is IfNode -> setOf(protocolFactory.constraint(this))

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
            val protocols = protocolSelection.viableProtocols(nameAnalysis.declaration(current))
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

    /** Generate cost constraints. */
    private fun Node.costConstraints(): Set<SelectionConstraint> =
        when (this) {
            // induce execution and communication costs
            is LetNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                    protocolSelection.viableProtocols(this),
                    nameAnalysis.reads(this.value).toList(),
                    { protocol -> costEstimator.executionCost(this.value, protocol) },
                    this.symbolicCost
                )
            }

            // induce storage and communication costs
            is DeclarationNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                    protocolSelection.viableProtocols(this),
                    this.arguments.filterIsInstance<ReadNode>(),
                    { protocol -> costEstimator.storageCost(this, protocol) },
                    this.symbolicCost
                )
            }

            // induce execution and communication costs
            is UpdateNode -> {
                val (fv, protocols) =
                    when (val objectDecl = nameAnalysis.declaration(this).declarationAsNode) {
                        is DeclarationNode ->
                            Pair(
                                FunctionVariable(nameAnalysis.enclosingFunctionName(objectDecl), objectDecl.name.value),
                                protocolSelection.viableProtocols(objectDecl)
                            )

                        is ParameterNode ->
                            Pair(
                                FunctionVariable(
                                    nameAnalysis.functionDeclaration(objectDecl).name.value,
                                    objectDecl.name.value
                                ),
                                protocolSelection.viableProtocols(objectDecl)
                            )

                        is ObjectDeclarationArgumentNode -> {
                            val param = nameAnalysis.parameter(objectDecl)
                            Pair(
                                FunctionVariable(
                                    nameAnalysis.functionDeclaration(param).name.value,
                                    objectDecl.name.value
                                ),
                                protocolSelection.viableProtocols(param)
                            )
                        }

                        else -> throw UnknownObjectDeclarationError(objectDecl)
                    }

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
                    protocolSelection.viableProtocols(parameter),
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
                        val guardProtocols = protocolSelection.viableProtocols(guardDecl)
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(this)

                        // map from declaration/let nodes to viable protocols
                        val viableProtocolMap: Map<FunctionVariable, Set<Protocol>> =
                            this.letNodes().map { letNode ->
                                Pair(
                                    FunctionVariable(enclosingFunctionName, letNode.temporary.value),
                                    protocolSelection.viableProtocols(letNode)
                                )
                            }.union(
                                this.declarationNodes().map { decl ->
                                    Pair(
                                        FunctionVariable(enclosingFunctionName, decl.name.value),
                                        protocolSelection.viableProtocols(decl)
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
                        val msgProtocols = protocolSelection.viableProtocols(msgDecl)
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

    /** Protocol selection. */
    fun select(): (FunctionName, Variable) -> Protocol {
        val solver = ctx.mkOptimize()
        val constraints = mutableSetOf<SelectionConstraint>()

        val reachableFunctionNames = nameAnalysis.reachableFunctions(main)
        val reachableFunctions = program.functions.filter { f -> reachableFunctionNames.contains(f.name.value) }

        for (function in reachableFunctions) {
            // select for function parameters first
            for (parameter in function.parameters) {
                constraints.add(
                    VariableIn(
                        FunctionVariable(function.name.value, parameter.name.value),
                        protocolSelection.viableProtocols(parameter)
                    )
                )
                constraints.add(protocolFactory.constraint(parameter))

                // induce costs for storing the parameter
                constraints.addAll(
                    protocolSelection.viableProtocols(parameter).map { protocol ->
                        val cost = costEstimator.storageCost(parameter, protocol)
                        Implies(
                            VariableIn(
                                FunctionVariable(function.name.value, parameter.name.value),
                                setOf(protocol)
                            ),
                            symbolicCostEqualsInt(parameter.symbolicCost, cost)
                        )
                    }.toSet()
                )
            }

            // then select for function bodies
            constraints.addAll(function.body.constraints())
        }

        // finally select for main process
        constraints.addAll(main.constraints())

        // build variable and protocol maps
        val letNodes: Map<LetNode, IntExpr> =
            reachableFunctions
                .flatMap { f -> f.letNodes() }
                .plus(main.letNodes())
                .map { letNode: LetNode ->
                    letNode to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }.toMap()

        val tempViables: Set<Protocol> =
            letNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

        val declarationNodes: Map<DeclarationNode, IntExpr> =
            reachableFunctions
                .flatMap { f -> f.declarationNodes() }
                .plus(main.declarationNodes())
                .map { decl: DeclarationNode ->
                    decl to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }.toMap()

        val declViables: Set<Protocol> =
            declarationNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

        val parameterNodes: Map<ParameterNode, IntExpr> =
            reachableFunctions.flatMap { function ->
                function.parameters.map { parameter ->
                    parameter to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }
            }.toMap()

        // All possible viable protocols that can be selected for parameters
        val parameterViables: Set<Protocol> =
            parameterNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

        val pmap: BiMap<Protocol, Int> =
            tempViables
                .union(declViables)
                .union(parameterViables)
                .withIndex().map {
                    it.value to it.index
                }.toMap().toBiMap()

        val varMap: BiMap<FunctionVariable, IntExpr> =
            (letNodes.mapKeys {
                FunctionVariable(nameAnalysis.enclosingFunctionName(it.key), it.key.temporary.value)
            }.plus(
                declarationNodes.mapKeys {
                    FunctionVariable(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
                }
            ).plus(
                parameterNodes.mapKeys {
                    val functionName = nameAnalysis.functionDeclaration(it.key).name.value
                    FunctionVariable(functionName, it.key.name.value)
                }
            )).toBiMap()

        val pmapExpr = pmap.mapValues { ctx.mkInt(it.value) as IntExpr }.toBiMap()

        // load selection constraints into Z3
        for (constraint in constraints) {
            solver.Add(constraint.boolExpr(ctx, varMap, pmapExpr))
        }

        if (varMap.values.isNotEmpty()) {
            // load cost constraints into Z3; build integer expression to minimize
            val weights = costEstimator.featureWeights()

            val programCostFeatures: Cost<SymbolicCost> =
                reachableFunctions.fold(main.symbolicCost) { acc, f -> acc.concat(f.body.symbolicCost) }

            val totalCost =
                programCostFeatures.features.entries
                    .fold(CostLiteral(0) as SymbolicCost) { acc, c ->
                        CostAdd(acc, CostMul(CostLiteral(weights[c.key]!!.cost), c.value))
                    }
            val costExpr = totalCost.arithExpr(ctx, varMap, pmapExpr)

            val totalCostSymvar = ctx.mkFreshConst("total_cost", ctx.intSort) as IntExpr
            solver.Add(ctx.mkEq(totalCostSymvar, costExpr))
            solver.MkMinimize(totalCostSymvar)

            if (solver.Check() == Status.SATISFIABLE) {
                val model = solver.model

                val interpMap: Map<FunctionVariable, Int> =
                    varMap.mapValues { e ->
                        (model.getConstInterp(e.value) as IntNum).int
                    }

                fun eval(f: FunctionName, v: Variable): Protocol {
                    val fvar = FunctionVariable(f, v)
                    return interpMap[fvar]?.let { protocolIndex ->
                        pmap.inverse[protocolIndex] ?: throw NoProtocolIndexMapping(protocolIndex)
                    } ?: throw NoVariableSelectionSolutionError(f, v)
                }

                return ::eval

            } else {
                throw NoSelectionSolutionError()
            }
        } else {
            return { f: FunctionName, v: Variable ->
                throw NoVariableSelectionSolutionError(f, v)
            }
        }
    }
}

fun selectProtocolsWithZ3(
    program: ProgramNode,
    main: ProcessDeclarationNode,
    protocolFactory: ProtocolFactory,
    costEstimator: CostEstimator<IntegerCost>
): (FunctionName, Variable) -> Protocol {
    val ctx = Context()
    val ret =
        Z3Selection(program, main, protocolFactory, costEstimator, ctx).select()
    ctx.close()
    return ret
}
