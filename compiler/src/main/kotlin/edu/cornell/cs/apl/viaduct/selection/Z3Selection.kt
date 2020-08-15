package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.ArithExpr
import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.toBiMap
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
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

                        else -> throw Exception("impossible case")
                    }

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

        fun viableProtocols(node: LetNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

        fun viableProtocols(node: DeclarationNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

        fun viableProtocols(node: ParameterNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()
    }

    private fun Node.constraints(): Set<SelectionConstraint> {
        val s = when (this) {
            is LetNode ->
                setOf(
                    VariableIn(this.temporary.value, protocolSelection.viableProtocols(this)),
                    protocolFactory.constraint(this)
                )
            is DeclarationNode ->
                setOf(
                    VariableIn(this.name.value, protocolSelection.viableProtocols(this)),
                    protocolFactory.constraint(this)
                )

            is ExpressionArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                nameAnalysis
                    .reads(this)
                    .map { read -> nameAnalysis.declaration(read) }
                    .map { letNode -> VariableEquals(letNode.temporary.value, parameter.name.value) }
                    .toSet()
            }

            is ObjectReferenceArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                setOf(VariableEquals(this.variable.value, parameter.name.value))
            }

            is OutParameterArgumentNode -> {
                val parameter = nameAnalysis.parameter(this)
                setOf(VariableEquals(this.parameter.value, parameter.name.value))
            }

            else -> setOf()
        }

        return s.union(
            this.children.map { it.constraints() }.unions()
        )
    }

    private val letNodes: Map<LetNode, IntExpr> =
        program.letNodes().map {
            it to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
        }.toMap()

    // All possible viable protocols that can be selected for temporaries.
    private val tempViables: Set<Protocol> =
        letNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

    private val declarationNodes: Map<DeclarationNode, IntExpr> =
        program.declarationNodes().map {
            it to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
        }.toMap()

    // All possible viable protocols that can be selected for objects
    private val declViables: Set<Protocol> =
        declarationNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

    private val parameterNodes: Map<ParameterNode, IntExpr> =
        program.functions.flatMap { function ->
            function.parameters.map { parameter ->
                parameter to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
            }
        }.toMap()

    // All possible viable protocols that can be selected for parameters
    private val parameterViables: Set<Protocol> =
        parameterNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

    /** Listing of all distinct protocols in question for the program. **/
    private val pmap: BiMap<Protocol, Int> =
        tempViables
            .union(declViables)
            .union(parameterViables)
            .withIndex().map {
                it.value to it.index
            }.toMap().toBiMap()

    /** Listing of all distinct variables in question for the program.
     */

    private val varMap: BiMap<Variable, IntExpr> =
        (letNodes.mapKeys {
            it.key.temporary.value
        }.plus(
            declarationNodes.mapKeys {
                it.key.name.value
            }
        ).plus(
            parameterNodes.mapKeys {
                it.key.name.value
            }
        )).toBiMap()

    /** Naive cost for selecting a protocol coded by the int p, which is equal to the cost of the corresponding protocol
    (as defined by the protocolCost function) */
    private fun symbolicCost(p: IntExpr): IntExpr {
        return pmap.toList().fold(ctx.mkInt(0) as IntExpr) { acc, x ->
            ctx.mkITE(ctx.mkEq(p, ctx.mkInt(x.second)), ctx.mkInt(protocolCost(x.first)), acc) as IntExpr
        }
    }

    fun select(): (Variable) -> Protocol {
        val solver = ctx.mkOptimize()
        val constraints = mutableSetOf<SelectionConstraint>()

        for (function in program.functions) {
            // select for function parameters first
            for (parameter in function.parameters) {
                constraints.add(VariableIn(parameter.name.value, protocolSelection.viableProtocols(parameter)))
                constraints.add(protocolFactory.constraint(parameter))
            }

            // then select for function bodies
            constraints.addAll(function.body.constraints())
        }

        // finally select for main process
        constraints.addAll(processDeclaration.constraints())

        for (constraint in constraints) {
            solver.Add(constraint.boolExpr(ctx, varMap, pmap.mapValues { ctx.mkInt(it.value) }.toBiMap()))
        }

        // TODO: this cost metric is particularly naive; it is simply the sum of costs of protocols for each selection.
        val cost = ctx.mkAdd(* (varMap.values.map { symbolicCost(it) as ArithExpr }).toTypedArray())
        solver.MkMinimize(cost)

        if (solver.Check() == Status.SATISFIABLE) {
            var model = solver.model
            val interpMap: Map<Variable, Int> =
                varMap.mapValues { e ->
                    (model.getConstInterp(e.value) as IntNum).int
                }

            fun eval(v: Variable): Protocol {
                if (interpMap.containsKey(v)) {
                    return pmap.inverse.get(interpMap[v]) ?: throw error("Protocol now found")
                } else {
                    throw SelectionError("Query for variable not contained in varMap: $v")
                }
            }
            return ::eval
        } else {
            // TODO: error class
            throw error("Solver could not find solution")
        }
    }
}

fun selectProtocolsWithZ3(
    program: ProgramNode,
    processDeclaration: ProcessDeclarationNode,
    protocolFactory: ProtocolFactory,
    protocolCost: (Protocol) -> Int
): (Variable) -> Protocol {
    val ctx = Context()
    val ret =
        Z3Selection(program, processDeclaration, protocolFactory, protocolCost, ctx).select()
    ctx.close()
    return ret
}

/**
 * Thrown when an error is found during protocol selection.
 *
 * @param info Description of the error.
 */
private class SelectionError(
    val info: String
) : Error(), PrettyPrintable {
    override val asDocument: Document
        get() = Document(info)
}
