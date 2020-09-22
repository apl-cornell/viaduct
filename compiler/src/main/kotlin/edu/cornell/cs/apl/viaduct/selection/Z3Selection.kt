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
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.util.unions

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
    private val processDeclaration: ProcessDeclarationNode,
    private val protocolFactory: ProtocolFactory,
    private val protocolCost: (Protocol) -> Int,
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

                else ->
                    protocolFactory.viableProtocols(this)
            }
        }

        private val DeclarationNode.viableProtocols: Set<Protocol> by attribute {
            protocolFactory.viableProtocols(this)
        }

        private val ParameterNode.viableProtocols: Set<Protocol> by attribute {
            protocolFactory.viableProtocols(this)
        }

        fun viableProtocols(node: LetNode): Set<Protocol> {
            val label = informationFlowAnalysis.label(node)
            return node.viableProtocols.filter {
                it.authority(hostTrustConfiguration).actsFor(label)
            }.toSet()
        }

        fun viableProtocols(node: DeclarationNode): Set<Protocol> {
            val label = informationFlowAnalysis.label(node)
            return node.viableProtocols.filter {
                it.authority(hostTrustConfiguration).actsFor(label)
            }.toSet()
        }

        fun viableProtocols(node: ParameterNode): Set<Protocol> {
            val label = informationFlowAnalysis.label(node)
            return node.viableProtocols.filter {
                it.authority(hostTrustConfiguration).actsFor(label)
            }.toSet()
        }
    }

    private fun Node.constraints(): Set<SelectionConstraint> {
        val s = when (this) {
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
                                    )
                                )

                            is ParameterNode ->
                                setOf(
                                    VariableEquals(
                                        FunctionVariable(enclosingFunctionName, objectDecl.name.value),
                                        FunctionVariable(enclosingFunctionName, this.temporary.value)
                                    )
                                )

                            is ObjectDeclarationArgumentNode -> {
                                val param = nameAnalysis.parameter(objectDecl)
                                setOf(
                                    VariableEquals(
                                        FunctionVariable(nameAnalysis.functionDeclaration(param).name.value, param.name.value),
                                        FunctionVariable(enclosingFunctionName, this.temporary.value)
                                    )
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

        return s.union(
            this.children.map { it.constraints() }.unions()
        )
    }

    private fun computeProtocolAndVariableMaps(
        main: ProcessDeclarationNode
    ): Pair<BiMap<Protocol, Int>, BiMap<FunctionVariable, IntExpr>> {
        val reachableFunctionNames = nameAnalysis.reachableFunctions(main)
        val reachableFunctions = program.functions.filter { f -> reachableFunctionNames.contains(f.name.value) }

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

        return Pair(pmap, varMap)
    }

    fun select(): (FunctionName, Variable) -> Protocol {
        val solver = ctx.mkOptimize()
        val constraints = mutableSetOf<SelectionConstraint>()

        val reachableFunctions = nameAnalysis.reachableFunctions(processDeclaration)
        for (function in program.functions) {
            if (reachableFunctions.contains(function.name.value)) {
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
        }

        // finally select for main process
        constraints.addAll(processDeclaration.constraints())

        val (pmap, varMap) = computeProtocolAndVariableMaps(processDeclaration)

        /** Naive cost for selecting a protocol coded by the int p, which is equal to the cost of the corresponding protocol
        (as defined by the protocolCost function) */
        // TODO: this cost metric is particularly naive; it is simply the sum of costs of protocols for each selection.
        val symbolicCost: (IntExpr) -> IntExpr = { p ->
            pmap.toList().fold(ctx.mkInt(0) as IntExpr) { acc, x ->
                ctx.mkITE(ctx.mkEq(p, ctx.mkInt(x.second)), ctx.mkInt(protocolCost(x.first)), acc) as IntExpr
            }
        }

        for (constraint in constraints) {
            solver.Add(constraint.boolExpr(ctx, varMap, pmap.mapValues { ctx.mkInt(it.value) }.toBiMap()))
        }

        if (varMap.values.isNotEmpty()) {
            val cost = ctx.mkAdd(* (varMap.values.map { symbolicCost(it) }).toTypedArray())
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
    processDeclaration: ProcessDeclarationNode,
    protocolFactory: ProtocolFactory,
    protocolCost: (Protocol) -> Int
): (FunctionName, Variable) -> Protocol {
    val ctx = Context()
    val ret =
        Z3Selection(program, processDeclaration, protocolFactory, protocolCost, ctx).select()
    ctx.close()
    return ret
}
