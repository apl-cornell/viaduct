package io.github.apl_cornell.viaduct.selection

import io.github.apl_cornell.apl.attributes.attribute
import io.github.apl_cornell.viaduct.analysis.InformationFlowAnalysis
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.analysis.descendantsIsInstance
import io.github.apl_cornell.viaduct.backends.cleartext.Local
import io.github.apl_cornell.viaduct.errors.InvalidProtocolAnnotationError
import io.github.apl_cornell.viaduct.errors.NoApplicableProtocolError
import io.github.apl_cornell.viaduct.errors.NoSelectionSolutionError
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.HostTrustConfiguration
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.intermediate.AssertionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BreakNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LiteralNode
import io.github.apl_cornell.viaduct.syntax.intermediate.Node
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.QueryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ReadNode
import io.github.apl_cornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode
import io.github.apl_cornell.viaduct.syntax.intermediate.VariableDeclarationNode
import io.github.apl_cornell.viaduct.util.FreshNameGenerator
import io.github.apl_cornell.viaduct.util.unions
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

private typealias CostVariable = String

class SelectionConstraintGenerator(
    private val program: ProgramNode,
    private val protocolFactory: ProtocolFactory,
    private val protocolComposer: ProtocolComposer,
    private val costEstimator: CostEstimator<IntegerCost>,
) {
    private val nameGenerator = FreshNameGenerator()
    private val hostTrustConfiguration = HostTrustConfiguration(program)
    private val nameAnalysis = NameAnalysis.get(program)
    private val informationFlowAnalysis = InformationFlowAnalysis.get(program)

    private val costChoiceMap =
        mutableMapOf<CostVariable, MutableList<Pair<SelectionConstraint, Cost<IntegerCost>>>>()

    // TODO: pc must be weak enough for the hosts involved in the selected protocols to read it
    fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> {
        val requiredAuthority = informationFlowAnalysis.label(node)
        val annotation = node.protocol?.value
        return if (annotation != null) {
            if (!annotation.authority(hostTrustConfiguration).actsFor(requiredAuthority))
                throw InvalidProtocolAnnotationError(node as Node)
            setOf(annotation)
        } else {
            protocolFactory.viableProtocols(node)
                .filter { it.authority(hostTrustConfiguration).actsFor(requiredAuthority) }
                .ifEmpty { throw NoApplicableProtocolError(node as Node) }
                .toSet()
        }
    }

    private fun viableProtocolsAndVariable(node: VariableDeclarationNode): Pair<FunctionVariable, Set<Protocol>> {
        val functionVariable = when (node) {
            is LetNode ->
                FunctionVariable(nameAnalysis.enclosingFunctionName(node), node.name.value)

            is DeclarationNode ->
                FunctionVariable(nameAnalysis.enclosingFunctionName(node), node.name.value)

            is ParameterNode ->
                FunctionVariable(nameAnalysis.functionDeclaration(node).name.value, node.name.value)

            is ObjectDeclarationArgumentNode -> {
                val param = nameAnalysis.parameter(node)
                FunctionVariable(nameAnalysis.functionDeclaration(param).name.value, node.name.value)
            }
        }
        return functionVariable to viableProtocols(node)
    }

    private fun viableProtocolsAndVariable(node: UpdateNode): Pair<FunctionVariable, Set<Protocol>> {
        return viableProtocolsAndVariable(nameAnalysis.declaration(node))
    }

    private fun viableProtocolsAndVariable(node: OutParameterInitializationNode): Pair<FunctionVariable, Set<Protocol>> {
        return viableProtocolsAndVariable(nameAnalysis.declaration(node))
    }

    private val zeroSymbolicCost = costEstimator.zeroCost().map { CostLiteral(0) }

    private fun addCostChoice(variable: CostVariable, guard: SelectionConstraint, cost: Cost<IntegerCost>) {
        if (!this.costChoiceMap.containsKey(variable)) {
            this.costChoiceMap[variable] = mutableListOf(Pair(guard, cost))
        } else {
            this.costChoiceMap[variable]!!.add(Pair(guard, cost))
        }
    }

    private fun getCostChoice(variable: CostVariable): Cost<SymbolicCost> =
        this.costChoiceMap[variable]?.let { choices ->
            zeroSymbolicCost.featureMap { feature, _ ->
                CostChoice(
                    choices.map { choice ->
                        Pair(choice.first, CostLiteral(choice.second.features[feature]!!.cost))
                    }
                )
            }
        } ?: throw NoSelectionSolutionError(program)

    private fun Cost<SymbolicCost>.featureSum(): SymbolicCost {
        val weights = costEstimator.featureWeights()
        return this.features.entries.fold(CostLiteral(0) as SymbolicCost) { acc, c ->
            CostAdd(acc, CostMul(weights[c.key]!!.cost, c.value))
        }
    }

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

            is HostDeclarationNode -> zeroSymbolicCost

            is BlockNode ->
                this.statements
                    .fold(zeroSymbolicCost) { acc, childStmt ->
                        acc.concat(childStmt.symbolicCost)
                    }

            is IfNode -> {
                val thenCost = this.thenBranch.symbolicCost
                val elseCost = this.elseBranch.symbolicCost
                zeroSymbolicCost.featureMap { feature, _ ->
                    CostMax(thenCost.features[feature]!!, elseCost.features[feature]!!)
                }
            }

            is InfiniteLoopNode ->
                this.body.symbolicCost.map { f -> CostMul(10, f) }

            // TODO: handle this later, recursive functions are tricky
            is FunctionCallNode -> zeroSymbolicCost

            is BreakNode -> zeroSymbolicCost

            is AssertionNode -> zeroSymbolicCost

            is ExpressionNode -> zeroSymbolicCost

            is SimpleStatementNode -> getCostChoice(this.costVariable)

            else -> zeroSymbolicCost
        }
    }

    private val SimpleStatementNode.costVariable by attribute { nameGenerator.getFreshName("cost") }

    /** Symbolic variables that specify whether a host is participating in the execution of a statement. */
    private val Node.participatingHosts: Map<Host, HostVariable> by attribute {
        program.hosts.associateWith { HostVariable(nameGenerator.getFreshName("host")) }
    }

    private val IfNode.guardVisibilityFlag: GuardVisibilityFlag by attribute {
        GuardVisibilityFlag(nameGenerator.getFreshName("guard"))
    }

    /** Generate constraints for possible protocols. */
    private fun Node.selectionConstraints(): Iterable<SelectionConstraint> =
        when (this) {
            is ParameterNode ->
                setOf(protocolFactory.constraint(this)).plus(
                    variableInSet(
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
                                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                                    Local(rhs.host.value)
                                )
                            )
                        }

                        // Queries need to be executed in the same protocol as the object
                        is QueryNode -> {
                            val enclosingFunctionName = nameAnalysis.enclosingFunctionName(this)

                            setOf(
                                variableInSet(
                                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                                    viableProtocols(this)
                                )
                            ).plus(
                                when (val objectDecl = nameAnalysis.declaration(rhs)) {
                                    is DeclarationNode ->
                                        VariableEquals(
                                            FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                            FunctionVariable(enclosingFunctionName, this.name.value)
                                        )

                                    is ParameterNode ->
                                        VariableEquals(
                                            FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                            FunctionVariable(enclosingFunctionName, this.name.value)
                                        )

                                    is ObjectDeclarationArgumentNode -> {
                                        val param = nameAnalysis.parameter(objectDecl)
                                        VariableEquals(
                                            FunctionVariable(
                                                nameAnalysis.functionDeclaration(param).name.value,
                                                param.name.value
                                            ),
                                            FunctionVariable(enclosingFunctionName, this.name.value)
                                        )
                                    }
                                }
                            )
                        }

                        else -> {
                            setOf(
                                variableInSet(
                                    FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
                                    viableProtocols(this)
                                )
                            )
                        }
                    }
                )

            is DeclarationNode ->
                setOf(
                    variableInSet(
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
                                VariableIn(fv, protocol),
                                iff(
                                    this.guardVisibilityFlag,
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
                            FunctionVariable(nameAnalysis.enclosingFunctionName(letNode), letNode.name.value),
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
     * Generate cost constraints for performing a computation (let nodes and updates).
     * Handles computation of communication costs.
     *
     * @param fv function-variable pair associated with the computation
     * @param protocols protocols that can implement the computation
     * @param reads the reads performed by the computation
     * @param baseCostFunction basic cost of computation node (no communication cost) as a function of its protocol
     * @param symbolicCost symbolic cost associated with the computation node.
     * */
    private fun generateComputationCostConstraints(
        stmt: SimpleStatementNode,
        fv: FunctionVariable,
        protocols: Set<Protocol>,
        reads: List<ReadNode>,
        baseCostFunction: (Protocol) -> Cost<IntegerCost>,
        costVariable: CostVariable
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

            val hostConstraints: SelectionConstraint =
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
                                    kv.value
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

                            addCostChoice(
                                costVariable,
                                And(VariableIn(fv, protocol), argProtocolConstraints),
                                cost
                            )

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
                                    And(participatingHostsConstraint)
                                )
                            )
                        } else { // has invalid arg protocols
                            invalidArgProtocolSet.addAll(invalidArgProtocols)
                            setOf()
                        }
                    }.ands()
                } else { // no arguments; compute execution cost directly
                    addCostChoice(costVariable, VariableIn(fv, protocol), baseCostFunction(protocol))
                    True
                }

            val invalidArgProtocolConstraint: SelectionConstraint =
                Not(
                    invalidArgProtocolSet.map { argProtocol ->
                        VariableIn(
                            FunctionVariable(fv.function, argProtocol.first.temporary.value),
                            argProtocol.second
                        )
                    }.ors()
                )

            Implies(
                VariableIn(fv, protocol),
                And(hostConstraints, invalidArgProtocolConstraint)
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
                    this.costVariable
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
                    this.costVariable
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
                    this.costVariable
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
                    this.costVariable
                )
            }

            // induce communication costs from message protocol to output protocol
            // TODO: partition by participating hosts
            is OutputNode -> {
                when (val msg = this.message) {
                    is LiteralNode -> {
                        addCostChoice(this.costVariable, True, costEstimator.zeroCost())
                        setOf()
                    }

                    is ReadNode -> {
                        val msgDecl = nameAnalysis.declaration(msg)
                        val enclosingFunctionName = nameAnalysis.enclosingFunctionName(msgDecl)
                        val msgProtocols = viableProtocols(msgDecl)
                        val outputProtocol = Local(this.host.value)

                        msgProtocols.flatMap { msgProtocol ->
                            if (protocolComposer.canCommunicate(msgProtocol, outputProtocol)) {
                                addCostChoice(
                                    this.costVariable,
                                    VariableIn(
                                        FunctionVariable(enclosingFunctionName, msgDecl.name.value),
                                        msgProtocol
                                    ),
                                    costEstimator.communicationCost(msgProtocol, outputProtocol)
                                )
                                setOf()
                            } else {
                                // if the message protocol can't communicate to Local,
                                // then it should not be selected for
                                setOf(
                                    Not(
                                        VariableIn(
                                            FunctionVariable(enclosingFunctionName, msgDecl.name.value),
                                            msgProtocol
                                        )
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
                VariableIn(fv, protocol),
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
                    program.hostDeclarations.associate {
                        it.name.value to (Literal(false) as SelectionConstraint)
                    }
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
                                                        guardProtocol
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }.ands()

                            // only turn on visibility constraints when
                            // the guard visibility flag is enabled
                            setOf(Implies(this.guardVisibilityFlag, visibilityConstraints))
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
    private fun Node.constraints(): Set<SelectionConstraint> =
        this.selectionConstraints()
            .union(this.costConstraints())
            .union(this.participatingHostConstraints())
            .union(this.children.map { it.constraints() }.unions())

    fun getSelectionProblem(): SelectionProblem {
        val selectionConstraints = program.constraints()
        val cost = program.symbolicCost.featureSum()

        val costMap =
            program.descendantsIsInstance<FunctionDeclarationNode>().map { funDecl ->
                funDecl to funDecl.symbolicCost.featureSum()
            }.plus(
                program.descendantsIsInstance<InfiniteLoopNode>().map { loopNode ->
                    loopNode to loopNode.symbolicCost.featureSum()
                }
            ).plus(
                program.descendantsIsInstance<IfNode>().map { ifNode ->
                    ifNode to ifNode.symbolicCost.featureSum()
                }
            ).plus(
                program.descendantsIsInstance<SimpleStatementNode>().map { sstmt ->
                    sstmt to sstmt.symbolicCost.featureSum()
                }
            ).toMap()

        return SelectionProblem(selectionConstraints, cost, costMap)
    }
}
