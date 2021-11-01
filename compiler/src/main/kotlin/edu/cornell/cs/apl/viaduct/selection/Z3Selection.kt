package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Global
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.mutableBiMapOf
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.errors.NoProtocolIndexMapping
import edu.cornell.cs.apl.viaduct.errors.NoSelectionSolutionError
import edu.cornell.cs.apl.viaduct.errors.NoVariableSelectionSolutionError
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Z3Selection")

enum class CostMode { MINIMIZE, MAXIMIZE }

/**
 * This class performs splitting by using Z3. It operates as follows:
 *
 * - First, it collects constraints on protocol selection from the [ProtocolFactory]. For each let or declaration,
 *      the factory outputs two things: first, it outputs a set of viable protocols for that variable. Second,
 *      it can output a number of custom constraints on selection for that variable which are forwarded to Z3.
 *      (For the simple factory, the custom constraints are trivial, as we have not yet constrained which protocols
 *      can talk to whom.)
 * - Second, it exports these constraints to Z3. The selection problem is encoded as follows:
 *      - We assign each possible viable protocol a unique integer index. Call this index i(p).
 *      - For each variable, we create a fresh integer constant. Call this constant c(v).
 *      - For each variable v with viable protocols P, we constrain that c(v) is contained in the image set of P under i.
 *      - For each variable v, we constrain c(v) relative to the custom constraints output by the factory.
 * - Third, we ask Z3 to optimize relative to a cost metric. The cost metric is provided by [costEstimator], which
 *   will represent cost using a set of features. At a high level, the cost estimator approximates cost by:
 *      - The cost of storing data in a protocol
 *      - The cost of executing computations in a protocol
 *      - Estimating the communication cost between one protocol reading data from another protocol
 */
private class Z3Selection(
    private val ctx: Context,
    private val costMode: CostMode,
    private val dumpMetadata: (Map<Node, PrettyPrintable>) -> Unit
) : SelectionConstraintSolver {
    private companion object {
        init {
            // Use old arithmetic solver to fix regression introduced in Z3 v4.8.9
            Global.setParameter("smt.arith.solver", "2")
        }
    }

    val nameGenerator = FreshNameGenerator()

    /** Convert a SelectionConstraint into a Z3 BoolExpr. **/
    private fun boolExpr(
        constraint: SelectionConstraint,
        ctx: Context,
        vmap: BiMap<FunctionVariable, IntExpr>,
        boolVarMap: Map<String, BoolExpr>,
        protocolMap: BiMap<Protocol, Int>
    ): BoolExpr {
        return when (constraint) {
            is True -> ctx.mkTrue()
            is False -> ctx.mkFalse()
            is HostVariable -> boolVarMap[constraint.variable]!!
            is GuardVisibilityFlag -> boolVarMap[constraint.variable]!!
            is Literal -> ctx.mkBool(constraint.literalValue)
            is Implies ->
                ctx.mkImplies(
                    boolExpr(constraint.lhs, ctx, vmap, boolVarMap, protocolMap),
                    boolExpr(constraint.rhs, ctx, vmap, boolVarMap, protocolMap)
                )

            is Or ->
                constraint.props.fold(ctx.mkFalse()) { acc, prop ->
                    ctx.mkOr(acc, boolExpr(prop, ctx, vmap, boolVarMap, protocolMap))
                }

            is And ->
                constraint.props.fold(ctx.mkTrue()) { acc, prop ->
                    ctx.mkAnd(acc, boolExpr(prop, ctx, vmap, boolVarMap, protocolMap))
                }

            is Not -> ctx.mkNot(boolExpr(constraint.rhs, ctx, vmap, boolVarMap, protocolMap))
            is VariableIn -> ctx.mkEq(vmap[constraint.variable], ctx.mkInt(protocolMap[constraint.protocol]!!))
            is VariableEquals -> ctx.mkEq(vmap[constraint.var1], vmap[constraint.var2])
        }
    }

    /** Convert a CostExpression into a Z3 ArithExpr. */
    private fun arithExpr(
        symCost: SymbolicCost,
        ctx: Context,
        fvMap: BiMap<FunctionVariable, IntExpr>,
        boolVarMap: Map<String, BoolExpr>,
        protocolMap: BiMap<Protocol, Int>
    ): Pair<IntExpr, BoolExpr> =
        when (symCost) {
            is CostLiteral -> Pair(ctx.mkInt(symCost.cost), ctx.mkTrue())

            is CostAdd -> {
                val (lhsExpr, constrsL) = arithExpr(symCost.lhs, ctx, fvMap, boolVarMap, protocolMap)
                val (rhsExpr, constrsR) = arithExpr(symCost.rhs, ctx, fvMap, boolVarMap, protocolMap)
                Pair(ctx.mkAdd(lhsExpr, rhsExpr) as IntExpr, ctx.mkAnd(constrsL, constrsR))
            }

            is CostMul -> {
                val (rhsExpr, constrs) = arithExpr(symCost.rhs, ctx, fvMap, boolVarMap, protocolMap)
                Pair(ctx.mkMul(ctx.mkInt(symCost.lhs), rhsExpr) as IntExpr, constrs)
            }

            is CostMax -> {
                val (lhsExpr, constrsL) = arithExpr(symCost.lhs, ctx, fvMap, boolVarMap, protocolMap)
                val (rhsExpr, constrsR) = arithExpr(symCost.rhs, ctx, fvMap, boolVarMap, protocolMap)
                Pair(
                    ctx.mkITE(ctx.mkGe(lhsExpr, rhsExpr), lhsExpr, rhsExpr) as IntExpr,
                    ctx.mkAnd(constrsL, constrsR)
                )
            }

            is CostChoice -> {
                val costVarName = this.nameGenerator.getFreshName("cost")
                val costVar = ctx.mkFreshConst(costVarName, ctx.intSort) as IntExpr
                Pair(
                    costVar,
                    ctx.mkAnd(
                        *symCost.choices.map { choice ->
                            val guardExpr = boolExpr(choice.first, ctx, fvMap, boolVarMap, protocolMap)
                            val (costExpr, costConstrs) = arithExpr(choice.second, ctx, fvMap, boolVarMap, protocolMap)
                            ctx.mkImplies(guardExpr, ctx.mkAnd(ctx.mkEq(costVar, costExpr), costConstrs))
                        }.toTypedArray()
                    )
                )
            }

            /*
            private fun printMetadata(
                eval: (FunctionName, Variable) -> Protocol,
                model: Model,
                totalCostSymvar: IntExpr
            ) {
                val nodeCostFunc: (Node) -> Pair<Node, PrettyPrintable> = { node ->
                    val symcost = constraintGenerator.symbolicCost(node)
                    val nodeCostStr =
                        symcost.featureSum().evaluate(eval) { cvar ->
                            val interpValue = model.getConstInterp(cvar.variable)
                            assert(interpValue != null)
                            (interpValue as IntNum).int
                        }.toString()

                    val nodeProtocolStr =
                        when (node) {
                            is LetNode -> {
                                val enclosingFunc = nameAnalysis.enclosingFunctionName(node)
                                "protocol: ${eval(enclosingFunc, node.temporary.value).asDocument.print()}"
                            }

                            is DeclarationNode -> {
                                val enclosingFunc = nameAnalysis.enclosingFunctionName(node)
                                "protocol: ${eval(enclosingFunc, node.name.value).asDocument.print()}"
                            }

                            else -> ""
                        }

                    Pair(node, Document("cost: $nodeCostStr $nodeProtocolStr"))
                }

                val declarationNodes = reachableInstances { declarationNodes() }

                val letNodes = reachableInstances { letNodes() }

                val updateNodes = reachableInstances { updateNodes() }

                val outputNodes = reachableInstances { outputNodes() }

                val ifNodes = reachableInstances { ifNodes() }

                val loopNodes = reachableInstances { infiniteLoopNodes() }

                val totalCostMetadata =
                    Document("total cost: ${(model.getConstInterp(totalCostSymvar) as IntNum).int}")

                val costMetadata: Map<Node, PrettyPrintable> =
                    declarationNodes.asSequence().map { nodeCostFunc(it) }
                        .plus(letNodes.map { nodeCostFunc(it) })
                        .plus(updateNodes.map { nodeCostFunc(it) })
                        .plus(outputNodes.map { nodeCostFunc(it) })
                        .plus(ifNodes.map { nodeCostFunc(it) })
                        .plus(loopNodes.map { nodeCostFunc(it) })
                        .plus(reachableFunctions.map { nodeCostFunc(it) })
                        .plus(nodeCostFunc(main))
                        .plus(Pair(program, totalCostMetadata))
                        .toMap()

                dumpMetadata(costMetadata)
            }
            */
        }

    /** Protocol selection. */
    override fun solveSelectionProblem(problem: SelectionProblem)
        : ProtocolAssignment {
        val constraints = problem.constraints
        val programCost = problem.cost

        val solver = ctx.mkOptimize()

        val protocolMap = mutableBiMapOf<Protocol, Int>()
        val fvMap = mutableBiMapOf<FunctionVariable, IntExpr>()
        val boolVarMap = mutableMapOf<String, BoolExpr>()

        var protocolCounter = 1
        for (constraint in constraints) {
            for (fv in constraint.functionVariables()) {
                if (!fvMap.containsKey(fv)) {
                    val fvSymname = this.nameGenerator.getFreshName("${fv.function.name}_${fv.variable.name}")
                    fvMap[fv] = ctx.mkFreshConst(fvSymname, ctx.intSort) as IntExpr
                }
            }

            for (protocol in constraint.protocols()) {
                if (!protocolMap.containsKey(protocol)) {
                    protocolMap[protocol] = protocolCounter
                    protocolCounter++
                }
            }

            for (variable in constraint.variableNames()) {
                if (!boolVarMap.containsKey(variable)) {
                    val varName = this.nameGenerator.getFreshName(variable)
                    boolVarMap[variable] = ctx.mkFreshConst(varName, ctx.boolSort) as BoolExpr
                }
            }
        }

        if (fvMap.values.isNotEmpty()) {
            // load selection constraints into Z3
            for (constraint in constraints) {
                solver.Add(boolExpr(constraint, ctx, fvMap, boolVarMap, protocolMap))
            }

            val (costExpr, costConstrs) = arithExpr(programCost, ctx, fvMap, boolVarMap, protocolMap)
            solver.Add(costConstrs)

            when (costMode) {
                CostMode.MINIMIZE -> solver.MkMinimize(costExpr)
                CostMode.MAXIMIZE -> solver.MkMaximize(costExpr)
            }

            val symvarCount = fvMap.size + boolVarMap.size

            logger.info { "number of symvars: $symvarCount" }
            logger.info { "cost mode set to $costMode" }

            if (solver.Check() == Status.SATISFIABLE) {
                val model = solver.model
                val interpMap: Map<FunctionVariable, Int> =
                    fvMap.mapValues { e ->
                        (model.getConstInterp(e.value) as IntNum).int
                    }

                fun eval(fv: FunctionVariable): Protocol {
                    return interpMap[fv]?.let { protocolIndex ->
                        protocolMap.inverse[protocolIndex] ?: throw NoProtocolIndexMapping(protocolIndex)
                    } ?: throw NoVariableSelectionSolutionError(fv.function, fv.variable)
                }

                // printMetadata(::eval, model, totalCostSymvar)

                logger.info { "constraints satisfiable, extracted model" }

                return ::eval
            } else {
                throw NoSelectionSolutionError()
            }
        } else {
            return { (f: FunctionName, v: Variable) ->
                throw NoVariableSelectionSolutionError(f, v)
            }
        }
    }
}

fun selectProtocolsWithZ3(
    program: ProgramNode,
    protocolFactory: ProtocolFactory,
    protocolComposer: ProtocolComposer,
    costEstimator: CostEstimator<IntegerCost>,
    costMode: CostMode = CostMode.MINIMIZE,
    dumpMetadata: (Map<Node, PrettyPrintable>) -> Unit = {}
): (FunctionName, Variable) -> Protocol {
    if (program.hosts.isEmpty()) {
        throw NoHostDeclarationsError(program.sourceLocation.sourcePath)
    }

    val constraintGenerator = SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator)

    Context().use { context ->
        val selectionProblem = constraintGenerator.getSelectionProblem()
        val assignment =
            Z3Selection(context, costMode, dumpMetadata)
                .solveSelectionProblem(selectionProblem)

        return { f, v -> assignment(FunctionVariable(f, v)) }
    }
}
