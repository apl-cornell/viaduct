package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.toBiMap
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.analysis.objectDeclarationArgumentNodes
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

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
    private companion object {
        val MAIN_FUNCTION = FunctionName("#main#")
    }

    private val nameAnalysis = NameAnalysis.get(program)

    private val constraintGenerator = ConstraintGenerator(program, protocolFactory)

    private val letNodes: Map<LetNode, IntExpr> =
        program.letNodes().map {
            it to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
        }.toMap()

    private val declarationNodes: Map<DeclarationNode, IntExpr> =
        program.declarationNodes().map {
            it to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
        }.toMap()

    private val objectDeclarationArgumentNodes: Map<ObjectDeclarationArgumentNode, IntExpr> =
        program.objectDeclarationArgumentNodes().map {
            it to (ctx.mkFreshConst("o", ctx.intSort) as IntExpr)
        }.toMap()

    /** Listing of all distinct protocols in question for the program. We also ensure that all local
     * protocols are included in the map. **/
    private val pmap: BiMap<Protocol, Int> =
        protocolFactory.protocols().map { it.protocol }.toSet().withIndex().map {
            it.value to it.index
        }.toMap().toBiMap()

    private val parameterNodes: Map<ParameterNode, IntExpr> =
        program.functions.flatMap { function ->
            function.parameters.map { parameter ->
                parameter to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
            }
        }.toMap()

    /** Listing of all distinct variables in question for the program.
     */

    private val varMap: BiMap<FunctionVariable, IntExpr> =
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
        ).plus(
            objectDeclarationArgumentNodes.mapKeys {
                Pair(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
            }
        )).toBiMap()

    /** Naive cost for selecting a protocol coded by the int p, which is equal to the cost of the corresponding protocol
    (as defined by the protocolCost function) */
    private fun symbolicCost(p: IntExpr): IntExpr {
        return pmap.toList().fold(ctx.mkInt(0) as IntExpr) { acc, x ->
            ctx.mkITE(ctx.mkEq(p, ctx.mkInt(x.second)), ctx.mkInt(protocolCost(x.first)), acc) as IntExpr
        }
    }

    fun select(): (FunctionName, Variable) -> Protocol {
        val solver = ctx.mkOptimize()
        val constraints = mutableSetOf<SelectionConstraint>()

        for (function in program.functions) {
            // select for function parameters first
            for (parameter in function.parameters) {
                constraints.add(
                    VariableIn(
                        Pair(function.name.value, parameter.name.value),
                        constraintGenerator.viableProtocols(parameter)
                    )
                )
                constraints.add(protocolFactory.constraint(parameter))
            }

            // then select for function bodies
            constraints.addAll(constraintGenerator.constraints(function.body))
        }

        // finally select for main process
        constraints.addAll(constraintGenerator.constraints(processDeclaration))

        for (constraint in constraints) {
            solver.Add(constraint.boolExpr(ctx, varMap, pmap.mapValues { ctx.mkInt(it.value) }.toBiMap()))
        }

        // TODO: this cost metric is particularly naive; it is simply the sum of costs of protocols for each selection.
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
                if (interpMap.containsKey(fvar)) {
                    return pmap.inverse.get(interpMap[fvar]) ?: throw error("Protocol now found")
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
): (FunctionName, Variable) -> Protocol {
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
