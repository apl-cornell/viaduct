package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.ArithExpr
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
import edu.cornell.cs.apl.viaduct.errors.SelectionError
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode

fun <T> List<Set<T>>.unions(): Set<T> {
    return this.fold(setOf<T>()) { acc, s -> acc.union(s) }
}

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

class Z3Selection(
    val ctx: Context,
    val processDeclaration: ProcessDeclarationNode,
    val informationFlowAnalysis: InformationFlowAnalysis,
    val nameAnalysis: NameAnalysis,
    val factory: ProtocolFactory,
    val protocolCost: (Protocol) -> Int
) {
    private val hostTrustConfiguration = HostTrustConfiguration(nameAnalysis.tree.root)

    // TODO: pc must be weak enough for the hosts involved in the selected protocols to read it
    private val protocolSelection = object {
        private val LetNode.viableProtocols: Set<Protocol> by attribute {
            when (value) {
                is InputNode ->
                    setOf(Local(value.host.value))
                is QueryNode -> nameAnalysis.declaration(value).viableProtocols
                else ->
                    factory.viableProtocols(this)
            }
        }

        private val DeclarationNode.viableProtocols: Set<Protocol> by attribute {
            factory.viableProtocols(this)
        }

        fun viableProtocols(node: LetNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()

        fun viableProtocols(node: DeclarationNode): Set<Protocol> = node.viableProtocols.filter {
            it.authority(hostTrustConfiguration).actsFor(informationFlowAnalysis.label(node))
        }.toSet()
    }

    private fun Node.constraints(): Set<SelectionConstraint> {
        val s = when (this) {
            is LetNode ->
                setOf(
                    VariableIn(this.temporary.value, protocolSelection.viableProtocols(this)),
                    factory.constraint(this)
                )
            is DeclarationNode ->
                setOf(
                    VariableIn(this.variable.value, protocolSelection.viableProtocols(this)),
                    factory.constraint(this)
                )
            else -> setOf()
        }

        return s.union(
            this.children.map { it.constraints() }.unions()
        )
    }

    private val letNodes: Map<LetNode, IntExpr> =
        processDeclaration.letNodes().map {
            it to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
        }.toMap()

    // All possible viable protocols that can be selected for temporaries.
    private val tempViables: Set<Protocol> =
        letNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

    private val declarationNodes: Map<DeclarationNode, IntExpr> =
        processDeclaration.declarationNodes().map {
            it to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
        }.toMap()

    // All possible viable protocols that can be selected for objects
    private val declViables: Set<Protocol> =
        declarationNodes.keys.map { protocolSelection.viableProtocols(it) }.unions()

    /** Listing of all distinct protocols in question for the program. **/
    private val pmap: BiMap<Protocol, Int> =
        tempViables.union(declViables).withIndex().map {
            it.value to it.index
        }.toMap().toBiMap()

    /** Listing of all distinct variables in question for the program.
     */

    private val varMap: BiMap<Variable, IntExpr> =
        (letNodes.mapKeys {
            it.key.temporary.value
        }.plus(
            declarationNodes.mapKeys {
                it.key.variable.value
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
        var solver = ctx.mkOptimize()
        val constraints = processDeclaration.constraints()

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

fun Z3Select(
    processDeclaration: ProcessDeclarationNode,
    informationFlowAnalysis: InformationFlowAnalysis,
    nameAnalysis: NameAnalysis,
    factory: ProtocolFactory,
    protocolCost: (Protocol) -> Int
): (Variable) -> Protocol {
    val ctx = Context()
    val ret =
        Z3Selection(ctx, processDeclaration, informationFlowAnalysis, nameAnalysis, factory, protocolCost).select()
    ctx.close()
    return ret
}
