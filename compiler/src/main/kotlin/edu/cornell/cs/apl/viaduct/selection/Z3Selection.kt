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
import edu.cornell.cs.apl.viaduct.analysis.ifNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.analysis.outputNodes
import edu.cornell.cs.apl.viaduct.analysis.updateNodes
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
                is QueryNode ->
                    when (val declaration = nameAnalysis.declaration(value).declarationAsNode) {
                        is DeclarationNode -> declaration.viableProtocols

                        is ParameterNode -> declaration.viableProtocols

                        is ObjectDeclarationArgumentNode ->
                            nameAnalysis.parameter(declaration).viableProtocols

                        else -> throw UnknownObjectDeclarationError(declaration)
                    }

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

    private fun Node.selectionConstraints(): Set<SelectionConstraint> =
        when (this) {
            is LetNode ->
                when (val rhs = this.value) {
                    is QueryNode -> {
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(this)
                        when (val objectDecl = nameAnalysis.declaration(rhs).declarationAsNode) {
                            is DeclarationNode ->
                                setOf(
                                    VariableEquals(
                                        FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                        FunctionVariable(enclosingFunctionName, this.temporary.value)
                                    ),
                                    protocolFactory.constraint(this)
                                )

                            is ParameterNode ->
                                setOf(
                                    VariableEquals(
                                        FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                        FunctionVariable(enclosingFunctionName, this.temporary.value)
                                    ),
                                    protocolFactory.constraint(this)
                                )

                            is ObjectDeclarationArgumentNode -> {
                                val param = nameAnalysis.parameter(objectDecl)
                                setOf(
                                    VariableEquals(
                                        FunctionVariable(
                                            nameAnalysis.functionDeclaration(param).name.value,
                                            param.name.value
                                        ),
                                        FunctionVariable(enclosingFunctionName, this.temporary.value)
                                    ),
                                    protocolFactory.constraint(this)
                                )
                            }

                            else -> throw UnknownObjectDeclarationError(objectDecl)
                        }
                    }

                    else -> {
                        setOf(
                            VariableIn(
                                Pair(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                                protocolSelection.viableProtocols(this)
                            ),
                            protocolFactory.constraint(this)
                        )
                    }
                }

            is DeclarationNode ->
                setOf(
                    VariableIn(
                        Pair(nameAnalysis.enclosingFunctionName(this), this.name.value),
                        protocolSelection.viableProtocols(this)
                    ),
                    protocolFactory.constraint(this)
                )

            is IfNode -> setOf(protocolFactory.constraint(this))

            is ExpressionArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                nameAnalysis
                    .reads(this)
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
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(this), this.variable.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            is OutParameterArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                val parameterFunctionName = nameAnalysis.functionDeclaration(parameter).name.value
                setOf(
                    VariableEquals(
                        Pair(nameAnalysis.enclosingFunctionName(this), this.parameter.value),
                        Pair(parameterFunctionName, parameter.name.value)
                    )
                )
            }

            else -> setOf()
        }

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

    private val Node.symbolicCost: Cost<SymbolicCost> by attribute {
        costEstimator
            .zeroCost()
            .map { CostVariable(ctx.mkFreshConst("cost", ctx.intSort) as IntExpr) }
    }

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
                val cost =
                    baseCostFunction(protocol).concat(
                        argProtocolMap.values.fold(costEstimator.zeroCost()) { acc, argProtocol ->
                            acc.concat(costEstimator.communicationCost(argProtocol, protocol))
                        }
                    )

                val protocolConstraints: SelectionConstraint =
                    argProtocolMap.map { kv ->
                        VariableIn(FunctionVariable(fv.first, kv.key), setOf(kv.value))
                    }.plus(
                        setOf(VariableIn(fv, setOf(protocol)))
                    ).fold(Literal(true) as SelectionConstraint) { acc, c -> And(acc, c) }

                val costConstraints: SelectionConstraint =
                    symbolicCost.features.map { kv ->
                        CostEquals(
                            kv.value,
                            cost.features[kv.key]?.let { c -> CostLiteral(c.cost) }
                                ?: throw Error("No cost associated with feature ${kv.key}")
                        )
                    }.fold(Literal(true) as SelectionConstraint) { acc, c -> And(acc, c) }

                Implies(protocolConstraints, costConstraints)
            }
        }.toSet()
    }

    private fun symbolicCostEqualsSym(symCost: Cost<SymbolicCost>, intCost: Cost<SymbolicCost>): SelectionConstraint =
        symCost.features.map { kv ->
            CostEquals(
                kv.value,
                intCost.features[kv.key]
                    ?: throw Error("No cost associated with feature ${kv.key}")
            )
        }.fold(Literal(true) as SelectionConstraint) { acc, c -> And(acc, c) }

    private fun symbolicCostEqualsInt(symCost: Cost<SymbolicCost>, intCost: Cost<IntegerCost>): SelectionConstraint =
        symbolicCostEqualsSym(symCost, intCost.map { c -> CostLiteral(c.cost) })

    private fun Node.costConstraints(): Set<SelectionConstraint> =
        when (this) {
            is LetNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value),
                    protocolSelection.viableProtocols(this),
                    nameAnalysis.reads(this.value).toList(),
                    { protocol -> costEstimator.executionCost(this.value, protocol) },
                    this.symbolicCost
                )
            }

            is DeclarationNode -> {
                generateComputationCostConstraints(
                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                    protocolSelection.viableProtocols(this),
                    this.arguments.filterIsInstance<ReadNode>(),
                    { protocol -> costEstimator.storageCost(this, protocol) },
                    this.symbolicCost
                )
            }

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

            is ParameterNode -> {
                val enclosingFunctionName =
                    nameAnalysis.functionDeclaration(this).name.value

                protocolSelection.viableProtocols(this).map { protocol ->
                    val cost = costEstimator.storageCost(this, protocol)
                    Implies(
                        VariableIn(
                            FunctionVariable(enclosingFunctionName, this.name.value),
                            setOf(protocol)
                        ),
                        symbolicCostEqualsInt(this.symbolicCost, cost)
                    )
                }.toSet()
            }

            // generate cost constraints for when temporaries are read as guards
            is IfNode -> {
                when (val guard = this.guard) {
                    is LiteralNode -> setOf()

                    is ReadNode -> {
                        val guardDecl = nameAnalysis.declaration(guard)

                        // make this cover transitive closure of reads
                        val guardProtocols =
                            nameAnalysis.reads(guardDecl).flatMap { read ->
                                protocolSelection.viableProtocols(nameAnalysis.declaration(read))
                            }.union(
                                protocolSelection.viableProtocols(guardDecl)
                            )

                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(this)
                        val variableProtocolMap: Map<FunctionVariable, Set<Protocol>> =
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

                        val participatingProtocolMap: MutableMap<Protocol, MutableSet<FunctionVariable>> = mutableMapOf()
                        for (kv in variableProtocolMap) {
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
                                symbolicCostEqualsSym(this.symbolicCost, protocolCost)
                            )
                        }.toSet()
                    }
                }
            }

            is OutputNode -> {
                when (val msg = this.message) {
                    is LiteralNode -> setOf()

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

    private fun Node.constraints(): Set<SelectionConstraint> =
        this.selectionConstraints()
            .union(this.costConstraints())
            .union(this.children.map { it.constraints() }.unions())

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
                        Pair(function.name.value, parameter.name.value),
                        protocolSelection.viableProtocols(parameter)
                    )
                )
                constraints.add(protocolFactory.constraint(parameter))
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

        val updateNodes: List<UpdateNode> =
            reachableFunctions.flatMap { f -> f.updateNodes() }.plus(main.updateNodes())

        val outputNodes: List<OutputNode> =
            reachableFunctions.flatMap { f -> f.outputNodes() }.plus(main.outputNodes())

        val ifNodes: List<IfNode> =
            reachableFunctions.flatMap { f -> f.ifNodes() }.plus(main.ifNodes())

        val pmap: BiMap<Protocol, Int> =
            tempViables
                .union(declViables)
                .union(parameterViables)
                .withIndex().map {
                    it.value to it.index
                }.toMap().toBiMap()

        val varMap: BiMap<FunctionVariable, IntExpr> =
            (letNodes.mapKeys {
                Pair(nameAnalysis.enclosingFunctionName(it.key), it.key.temporary.value)
            }.plus(
                declarationNodes.mapKeys {
                    Pair(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
                }
            ).plus(
                parameterNodes.mapKeys {
                    val functionName = nameAnalysis.functionDeclaration(it.key).name.value
                    Pair(functionName, it.key.name.value)
                }
            )).toBiMap()

        // tally all costs associated with storage (declaration nodes) and computations (let nodes and update nodes)
        val zeroSymbolicCost = costEstimator.zeroCost().map { CostLiteral(0) }
        val programCost =
            letNodes.keys.fold(zeroSymbolicCost) { acc, letNode -> acc.concat(letNode.symbolicCost) }
                .concat(
                    declarationNodes.keys.fold(zeroSymbolicCost) { acc, decl -> acc.concat(decl.symbolicCost) }
                )
                .concat(
                    parameterNodes.keys.fold(zeroSymbolicCost) { acc, param -> acc.concat(param.symbolicCost) }
                )
                .concat(
                    updateNodes.fold(zeroSymbolicCost) { acc, update -> acc.concat(update.symbolicCost) }
                )
                .concat(
                    outputNodes.fold(zeroSymbolicCost) { acc, output -> acc.concat(output.symbolicCost) }
                )
                .concat(
                    ifNodes.fold(zeroSymbolicCost) { acc, ifNode -> acc.concat(ifNode.symbolicCost) }
                )

        val pmapExpr = pmap.mapValues { ctx.mkInt(it.value) as IntExpr }.toBiMap()

        // load selection constraints into Z3
        for (constraint in constraints) {
            solver.Add(constraint.boolExpr(ctx, varMap, pmapExpr))
        }

        if (varMap.values.isNotEmpty()) {
            // load cost constraints into Z3; build integer expression to minimize
            val weights = costEstimator.featureWeights()
            val cost =
                programCost.features.entries
                    .fold(CostLiteral(0) as SymbolicCost) { acc, c ->
                        CostAdd(acc, CostMul(CostLiteral(weights[c.key]!!.cost), c.value))
                    }
                    .arithExpr(ctx, varMap, pmapExpr)

            solver.MkMinimize(cost)

            if (solver.Check() == Status.SATISFIABLE) {
                val model = solver.model

                val interpMap: Map<FunctionVariable, Int> =
                    varMap.mapValues { e ->
                        (model.getConstInterp(e.value) as IntNum).int
                    }

                fun eval(f: FunctionName, v: Variable): Protocol {
                    val fvar = Pair(f, v)
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
