package io.github.apl_cornell.viaduct.selection

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Global
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import com.microsoft.z3.enumerations.Z3_lbool
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.mutableBiMapOf
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.util.FreshNameGenerator
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Z3Selection")

/**
 * Constraint problem using Z3. Z3 has an optimization module that can return models with minimal cost.
 */
class Z3SelectionProblemSolver : SelectionProblemSolver {
    companion object {
        init {
            // Use old arithmetic solver to fix regression introduced in Z3 v4.8.9
            Global.setParameter("smt.arith.solver", "2")
        }
    }

    override val solverName = "z3"

    private val nameGenerator = FreshNameGenerator()

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
        }

    /** Protocol selection. */
    override fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment? {
        Context().use { ctx ->
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
                solver.MkMinimize(costExpr)

                val symvarCount = fvMap.size + boolVarMap.size

                logger.info { "number of symvars: $symvarCount" }

                if (solver.Check() == Status.SATISFIABLE) {
                    val model = solver.model
                    val assignment =
                        ProtocolAssignment(
                            fvMap.mapValues { e ->
                                val protocolIndex = (model.getConstInterp(e.value) as IntNum).int
                                protocolMap.inverse.getValue(protocolIndex)
                            },
                            boolVarMap.mapValues { kv ->
                                (model.evaluate(kv.value, false) as BoolExpr).boolValue == Z3_lbool.Z3_L_TRUE
                            },
                            problem
                        )

                    logger.info { "constraints satisfiable, extracted model" }

                    return assignment
                } else {
                    return null
                }
            } else {
                return ProtocolAssignment(mapOf(), mapOf(), problem)
            }
        }
    }
}
