package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.UnknownObjectDeclarationError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.unions
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

class SelectionConstraintGenerator(
    private val program: ProgramNode,
    private val protocolFactory: ProtocolFactory,
    private val protocolComposer: ProtocolComposer,
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

    private fun viableProtocolsAndVariable(decl: ObjectDeclaration): Pair<FunctionVariable, Set<Protocol>> =
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

    private fun viableProtocolsAndVariable(node: LetNode): Pair<FunctionVariable, Set<Protocol>> {
        val enclosingFunction = nameAnalysis.enclosingFunctionName(node)
        return FunctionVariable(enclosingFunction, node.temporary.value) to viableProtocols(node)
    }

    private fun viableProtocolsAndVariable(node: UpdateNode): Pair<FunctionVariable, Set<Protocol>> {
        return viableProtocolsAndVariable(nameAnalysis.declaration(node))
    }

    private fun viableProtocolsAndVariable(node: OutParameterInitializationNode): Pair<FunctionVariable, Set<Protocol>> {
        return viableProtocolsAndVariable(nameAnalysis.declaration(node))
    }

    private val zeroSymbolicCost = costEstimator.zeroCost().map { CostLiteral(0) }

    /** lift [this] integer cost to symbolic cost. */
    private fun Cost<IntegerCost>.toSymbolicCost(): Cost<SymbolicCost> =
        this.map { CostLiteral(it.cost) }

    /** Symbolic cost associated with a node. */
    private val Node.symbolicCost: Cost<SymbolicCost> by attribute {
        when (this) {
            is ProgramNode ->
                this.declarations.fold(zeroSymbolicCost) { acc, decl ->
                    acc.concat(decl.symbolicCost)
                }

            is FunctionDeclarationNode -> this.body.symbolicCost

            // don't bother giving cost to parameters, since arguments already incur cost
            is ParameterNode -> zeroSymbolicCost

            is ProcessDeclarationNode -> this.body.symbolicCost

            is HostDeclarationNode -> zeroSymbolicCost

            is BlockNode ->
                this.statements
                    .fold(zeroSymbolicCost) { acc, childStmt ->
                        acc.concat(childStmt.symbolicCost)
                    }

            is IfNode ->
                zeroSymbolicCost.featureMap { feature, _ ->
                    val thenCost = this.thenBranch.symbolicCost[feature]!!
                    val elseCost = this.elseBranch.symbolicCost[feature]!!
                    CostMux(CostLessThanEqualTo(thenCost, elseCost), elseCost, thenCost)
                }

            is InfiniteLoopNode ->
                this.body.symbolicCost.map { f -> CostMul(CostLiteral(10), f) }

            // TODO: handle this later, recursive functions are tricky
            is FunctionCallNode -> zeroSymbolicCost

            is BreakNode -> zeroSymbolicCost

            is AssertionNode -> zeroSymbolicCost

            is SendNode -> zeroSymbolicCost

            is ExpressionNode -> zeroSymbolicCost

            else ->
                costEstimator
                    .zeroCost()
                    .map { CostVariable(ctx.mkFreshConst("cost_${this.asDocument.print()}", ctx.intSort) as IntExpr) }
        }
    }

    fun symbolicCost(node: Node) = node.symbolicCost

    /** Symbolic variables that specify whether a host is participating in the execution of a statement. */
    private val Node.participatingHosts: Map<Host, HostVariable> by attribute {
        program.hosts.map { host ->
            host to HostVariable(ctx.mkFreshConst("host", ctx.boolSort) as BoolExpr)
        }.toMap()
    }

    private val IfNode.guardVisiblityFlag: GuardVisibilityFlag by attribute {
        GuardVisibilityFlag(ctx.mkFreshConst("guard", ctx.boolSort) as BoolExpr)
    }

    /** Generate constraints for possible protocols. */
    private fun Node.selectionConstraints(): Iterable<SelectionConstraint> =
        when (this) {
            is ParameterNode ->
                setOf(protocolFactory.constraint(this)).plus(
                    VariableIn(
                        FunctionVariable(
                            nameAnalysis.functionDeclaration(this).name.value,
                            this.name.value
                        ),
                        viableProtocols(this)
                    )
                )

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

                        is ReceiveNode -> throw IllegalInternalCommunicationError(rhs)

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

            is UpdateNode -> setOf(protocolFactory.constraint(this))

            // generate constraints for guard visibility
            // used by the ABY/MPC factory to generate muxing constraints
            is IfNode -> {
                when (val guard = this.guard) {
                    is LiteralNode -> setOf()

                    is ReadNode -> {
                        val guardDeclaration = nameAnalysis.declaration(guard)
                        val (fv, protocols) = viableProtocolsAndVariable(guardDeclaration)
                        protocols.map { protocol ->
                            Implies(
                                VariableIn(fv, setOf(protocol)),
                                iff(
                                    this.guardVisiblityFlag,
                                    protocolFactory.guardVisibilityConstraint(protocol, this)
                                )
                            )
                        }
                    }
                }
            }

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

            is SendNode -> throw IllegalInternalCommunicationError(this)

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
        previous: PersistentMap<ReadNode, Protocol>,
        next: List<ReadNode>
    ):
        Set<PersistentMap<ReadNode, Protocol>> {
        return if (next.isEmpty()) {
            setOf(previous)
        } else {
            val current = next.first()
            val tail = next.subList(1, next.size)
            val protocols = viableProtocols(nameAnalysis.declaration(current))
            protocols.flatMap { protocol ->
                getArgumentViableProtocols(
                    previous.put(current, protocol),
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
    private fun symbolicCostEqualsSym(
        symCost: Cost<SymbolicCost>,
        symCost2: Cost<SymbolicCost>
    ):
        SelectionConstraint =
        symCost.features.map { kv ->
            CostEquals(
                kv.value,
                symCost2.features[kv.key]
                    ?: throw Error("No cost associated with feature ${kv.key}")
            )
        }.ands()

    /**
     * Generate constraints that set a symbolic cost equal to some integer cost.
     *
     * @param symCost: symbolic cost
     * @param intCost: integer cost
     * @return selection constraints that set the features of [symCost] and [intCost] equal.
     */
    private fun symbolicCostEqualsInt(
        symCost: Cost<SymbolicCost>,
        intCost: Cost<IntegerCost>
    ):
        SelectionConstraint =
        symbolicCostEqualsSym(symCost, intCost.toSymbolicCost())

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
        stmt: SimpleStatementNode,
        fv: FunctionVariable,
        protocols: Set<Protocol>,
        reads: List<ReadNode>,
        baseCostFunction: (Protocol) -> Cost<IntegerCost>,
        symbolicCost: Cost<SymbolicCost>
    ):
        Iterable<SelectionConstraint> {

        // cartesian product of all viable protocols for arguments
        val argProtocolMaps: Set<PersistentMap<ReadNode, Protocol>> =
            getArgumentViableProtocols(persistentMapOf(), reads)
                .filter { map -> map.isNotEmpty() }
                .toSet()

        // for each element in the cartesian product of viable protocols,
        // compute the cost using the cost estimator
        return protocols.map { protocol ->
            val invalidArgProtocolSet: MutableSet<Pair<ReadNode, Protocol>> = mutableSetOf()

            val costAndHostConstraints: SelectionConstraint =
                if (argProtocolMaps.isNotEmpty()) {
                    argProtocolMaps.flatMap { argProtocolMap ->
                        val invalidArgProtocols =
                            argProtocolMap
                                .filter { kv -> !protocolComposer.canCommunicate(kv.value, protocol) }
                                .toList()

                        val argProtocolConstraints: SelectionConstraint =
                            argProtocolMap.map { kv ->
                                VariableIn(
                                    FunctionVariable(fv.function, kv.key.temporary.value),
                                    setOf(kv.value)
                                )
                            }.ands()

                        if (invalidArgProtocols.isEmpty()) { // no invalid arg protocols
                            // estimate cost given a particular configuration of an executing protocol
                            // and protocols for arguments
                            // TODO: figure out why cost partitioned on participating hosts doesn't work
                            /*
                            val costMap: Map<Host, Cost<IntegerCost>> =
                                protocol.hosts.map { host ->
                                    val communicationCosts =
                                        argProtocolMap.values.fold(baseCostFunction(protocol)) { acc, argProtocol ->
                                            acc.concat(costEstimator.communicationCost(argProtocol, protocol, host))
                                        }

                                    host to communicationCosts
                                }.toMap()

                            val cost: Cost<SymbolicCost> =
                                costMap.map { kv ->
                                    kv.value.map { featureCost ->
                                        // only induce the cost if the host is participating
                                        CostMux(
                                            stmt.participatingHosts[kv.key]!!,
                                            CostLiteral(featureCost.cost),
                                            CostLiteral(0)
                                        )
                                    }
                                }.fold(baseCostFunction(protocol).toSymbolicCost()) { acc, hostCost ->
                                    acc.concat(hostCost)
                                }

                            val costConstraint: SelectionConstraint = symbolicCostEqualsSym(symbolicCost, cost)
                            */

                            val cost: Cost<IntegerCost> =
                                baseCostFunction(protocol).concat(
                                    argProtocolMap.values.fold(costEstimator.zeroCost()) { acc, argProtocol ->
                                        acc.concat(costEstimator.communicationCost(argProtocol, protocol))
                                    }
                                )

                            val costConstraint: SelectionConstraint = symbolicCostEqualsInt(symbolicCost, cost)

                            val participatingHostsConstraint: SelectionConstraint =
                                argProtocolMap.flatMap { argProtocol ->
                                    val argDeclaration = nameAnalysis.declaration(argProtocol.key)
                                    val events =
                                        if (protocolComposer.canCommunicate(argProtocol.value, protocol))
                                            (protocolComposer.communicate(argProtocol.value, protocol))
                                        else
                                            (ProtocolCommunication(setOf()))

                                    // if a protocol host is participating, every host sending a message to it must also be participating
                                    protocol.hosts.map { protocolHost ->
                                        val activatedReaderHosts =
                                            events
                                                .getHostReceives(protocolHost)
                                                .map { event -> event.send.host }
                                                .toSet()

                                        Implies(
                                            stmt.participatingHosts[protocolHost]!!,
                                            activatedReaderHosts.map { readerHost ->
                                                argDeclaration.participatingHosts[readerHost]!!
                                            }.ands()
                                        )
                                    }
                                }.ands()

                            setOf(
                                Implies(
                                    argProtocolConstraints,
                                    And(costConstraint, participatingHostsConstraint)
                                )
                            )
                        } else { // has invalid arg protocols
                            invalidArgProtocolSet.addAll(invalidArgProtocols)
                            setOf()
                        }
                    }.ands()
                } else { // no arguments; compute execution cost directly
                    symbolicCostEqualsInt(symbolicCost, baseCostFunction(protocol))
                }

            val invalidArgProtocolConstraint: SelectionConstraint =
                Not(
                    invalidArgProtocolSet.map { argProtocol ->
                        VariableIn(
                            FunctionVariable(fv.function, argProtocol.first.temporary.value),
                            setOf(argProtocol.second)
                        )
                    }.ors()
                )

            Implies(
                VariableIn(fv, setOf(protocol)),
                And(costAndHostConstraints, invalidArgProtocolConstraint)
            )
        }
    }

    /** Generate cost constraints. */
    private fun Node.costConstraints():
        Iterable<SelectionConstraint> =
        when (this) {
            /*
            is ParameterNode -> {
                val fv =
                    FunctionVariable(
                        nameAnalysis.functionDeclaration(this).name.value,
                        this.name.value
                    )

                viableProtocols(this).map { protocol ->
                    Implies(
                        VariableIn(fv, setOf(protocol)),
                        symbolicCostEqualsInt(symbolicCost, costEstimator.executionNode(this, protocol))
                    )
                }
            }
            */

            // induce execution and communication costs
            is LetNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                generateComputationCostConstraints(
                    this,
                    fv,
                    protocols,
                    nameAnalysis.reads(this.value).toList(),
                    { protocol -> costEstimator.executionCost(this, protocol) },
                    this.symbolicCost
                )
            }

            // induce storage and communication costs
            is DeclarationNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                generateComputationCostConstraints(
                    this,
                    fv,
                    protocols,
                    this.arguments.filterIsInstance<ReadNode>(),
                    { protocol -> costEstimator.executionCost(this, protocol) },
                    this.symbolicCost
                )
            }

            // induce execution and communication costs
            is UpdateNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                generateComputationCostConstraints(
                    this,
                    fv,
                    protocols,
                    this.arguments.filterIsInstance<ReadNode>(),
                    { protocol -> costEstimator.executionCost(this, protocol) },
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
                    this,
                    FunctionVariable(
                        nameAnalysis.functionDeclaration(parameter).name.value,
                        parameter.name.value
                    ),
                    viableProtocols(parameter),
                    reads,
                    { protocol -> costEstimator.executionCost(this, protocol) },
                    this.symbolicCost
                )
            }

            // induce communication costs from message protocol to output protocol
            // TODO: partition by participating hosts
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
                            if (protocolComposer.canCommunicate(msgProtocol, outputProtocol)) {
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
                            } else {
                                // if the message protocol can't communicate to Local,
                                // then it should not be selected for
                                Not(
                                    VariableIn(
                                        FunctionVariable(enclosingFunctionName, msgDecl.temporary.value),
                                        setOf(msgProtocol)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            else -> setOf()
        }

    private fun statementParticipatingHostConstraints(
        stmt: SimpleStatementNode,
        fv: FunctionVariable,
        protocols: Set<Protocol>
    ):
        Iterable<SelectionConstraint> {
        return protocols.map { protocol ->
            val mandatoryHosts = protocolComposer.mandatoryParticipatingHosts(protocol, stmt)
            Implies(
                VariableIn(fv, setOf(protocol)),
                program.hosts
                    .flatMap { host ->
                        when {
                            mandatoryHosts.contains(host) ->
                                setOf(stmt.participatingHosts[host]!!)

                            !protocol.hosts.contains(host) ->
                                setOf(Not(stmt.participatingHosts[host]!!))

                            else -> setOf()
                        }
                    }.ands()
            )
        }
    }

    /** Describes the relationships between hosts participating in execution of statements. */
    private fun Node.participatingHostConstraints():
        Iterable<SelectionConstraint> =
        when (this) {
            // a host participates in a block node if it participates in any of the children
            is BlockNode -> {
                this.statements.fold(
                    program.hostDeclarations.map {
                        it.name.value to (Literal(false) as SelectionConstraint)
                    }.toMap()
                ) { acc: Map<Host, SelectionConstraint>, stmt: StatementNode ->
                    acc.mapValues { kv -> Or(kv.value, stmt.participatingHosts[kv.key]!!) }
                }.map { kv ->
                    iff(kv.value, this.participatingHosts[kv.key]!!)
                }
            }

            // a host participates in a conditional if it participates in either branch
            is IfNode -> {
                this.thenBranch.participatingHosts.mapValues { kv ->
                    Or(kv.value, this.elseBranch.participatingHosts[kv.key]!!)
                }.map { kv ->
                    iff(kv.value, this.participatingHosts[kv.key]!!)
                }.plus(
                    // the guard must be visible to all hosts participating in the conditional
                    when (val guard = this.guard) {
                        is LiteralNode -> setOf()

                        is ReadNode -> {
                            val enclosingFunction = nameAnalysis.enclosingFunctionName(guard)
                            val guardDeclaration = nameAnalysis.declaration(guard)

                            val visibilityConstraints: SelectionConstraint =
                                viableProtocols(guardDeclaration).flatMap { guardProtocol ->
                                    val visibleGuardHosts = protocolComposer.visibleGuardHosts(guardProtocol)
                                    program.hosts.map { host ->
                                        if (visibleGuardHosts.contains(host)) {
                                            // if a host participates in a conditional,
                                            // then it must also participate in computing the guard
                                            Implies(
                                                this.participatingHosts[host]!!,
                                                guardDeclaration.participatingHosts[host]!!
                                            )
                                        } else {
                                            // if a host participates in a conditional but the guard is
                                            // not visible to it in a protocol, then the guard cannot
                                            // be executed in that protocol
                                            Implies(
                                                this.participatingHosts[host]!!,
                                                Not(
                                                    VariableIn(
                                                        FunctionVariable(
                                                            enclosingFunction,
                                                            guard.temporary.value
                                                        ),
                                                        setOf(guardProtocol)
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }.ands()

                            // only turn on visibility constraints when
                            // the guard visibility flag is enabled
                            setOf(Implies(this.guardVisiblityFlag, visibilityConstraints))
                        }
                    }
                )
            }

            // a host participates in a loop if it participates in the loop body
            is InfiniteLoopNode -> {
                this.body.participatingHosts.map { kv ->
                    iff(kv.value, this.participatingHosts[kv.key]!!)
                }
            }

            // a host participating in a loop must participate in breaking out of it, and vice versa
            is BreakNode -> {
                nameAnalysis.correspondingLoop(this).participatingHosts.map { kv ->
                    val hostVariable = this.participatingHosts[kv.key]!!
                    iff(kv.value, hostVariable)
                }
            }

            // TODO: handle parameters
            is FunctionCallNode -> {
                val functionDecl = nameAnalysis.declaration(this)

                // every host participating in the called function's body
                // must also participate in the function call
                functionDecl.body.participatingHosts.map { kv ->
                    Implies(kv.value, this.participatingHosts[kv.key]!!)
                }
            }

            is LetNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                statementParticipatingHostConstraints(this, fv, protocols)
            }

            is DeclarationNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                statementParticipatingHostConstraints(this, fv, protocols)
            }

            is UpdateNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                statementParticipatingHostConstraints(this, fv, protocols)
            }

            is OutParameterInitializationNode -> {
                val (fv, protocols) = viableProtocolsAndVariable(this)
                statementParticipatingHostConstraints(this, fv, protocols)
            }

            is OutputNode -> {
                setOf(
                    program.hosts.map { host ->
                        if (host == this.host.value) {
                            this.participatingHosts[host]!!
                        } else {
                            Not(this.participatingHosts[host]!!)
                        }
                    }.ands()
                )
            }

            else -> setOf()
        }

    /** Generate selection, cost, and participating host constraints. */
    private fun Node.constraints():
        Set<SelectionConstraint> =
        this.selectionConstraints()
            .union(this.costConstraints())
            .union(this.participatingHostConstraints())
            .union(this.children.map { it.constraints() }.unions())

    fun getConstraints(node: Node) = node.constraints()
}
