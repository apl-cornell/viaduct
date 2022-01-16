package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import gurobi.GRB
import gurobi.GRBEnv
import gurobi.GRBLinExpr
import gurobi.GRBModel
import gurobi.GRBVar
import java.util.Collections
import kotlin.math.max

/** Translate selection problem into an integer linear program and use the Gurobi solver to find a solution. */
class GurobiSelectionProblemSolver : SelectionProblemSolver {
    override val solverName = "gurobi"

    private val constraintMap = HashMap<SelectionConstraint, GRBVar>()
    private val assignmentVariableMap = HashMap<FunctionVariable, MutableList<Pair<Protocol, GRBVar>>>()
    private val variableEqualsSet = mutableSetOf<VariableEquals>()
    private val costMap = HashMap<SymbolicCost, GRBVar>()
    private var selectionVarNameCounter = 0
    private var costVarNameCounter = 0
    private var constraintNameCounter = 0

    private val env = GRBEnv()
    private val model = GRBModel(env)

    init {
        env.set(GRB.IntParam.OutputFlag, 0)
        env.start()
    }

    private fun generateSelectionVar(name: String): GRBVar {
        val varName = "$name$selectionVarNameCounter"
        selectionVarNameCounter++
        return model.addVar(0.0, 1.0, 0.0, GRB.BINARY, varName)
    }

    private fun generateCostVar(name: String): GRBVar {
        val varName = "$name$costVarNameCounter"
        costVarNameCounter++
        return model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, varName)
    }

    private fun generateConstraintName(): String {
        val name = "Constr$constraintNameCounter"
        constraintNameCounter++
        return name
    }

    private fun translateSelectionConstraint(constr: SelectionConstraint): GRBVar {
        return constraintMap[constr]
            ?: when (constr) {
                is And -> {
                    val childVars = constr.props.map { child -> translateSelectionConstraint(child) }
                    val selfVar = generateSelectionVar("And")

                    model.addGenConstrAnd(selfVar, childVars.toTypedArray(), generateConstraintName())

                    constraintMap[constr] = selfVar
                    selfVar
                }

                is Or -> {
                    val childVars = constr.props.map { child -> translateSelectionConstraint(child) }
                    val selfVar = generateSelectionVar("Or")
                    model.addGenConstrOr(selfVar, childVars.toTypedArray(), generateConstraintName())

                    constraintMap[constr] = selfVar
                    selfVar
                }

                is Implies -> {
                    val lhsVar = translateSelectionConstraint(constr.lhs)
                    val rhsVar = translateSelectionConstraint(constr.rhs)
                    val selfVar = generateSelectionVar("Implies")

                    // −1 ≤ 1− left + right −2 * self ≤ 0
                    val expr = GRBLinExpr()
                    expr.addConstant(1.0)
                    expr.addTerm(-1.0, lhsVar)
                    expr.addTerm(1.0, rhsVar)
                    expr.addTerm(-2.0, selfVar)

                    model.addConstr(-1.0, GRB.LESS_EQUAL, expr, generateConstraintName())
                    model.addConstr(expr, GRB.LESS_EQUAL, 0.0, generateConstraintName())

                    constraintMap[constr] = selfVar
                    selfVar
                }

                is Not -> {
                    val childVar = translateSelectionConstraint(constr.rhs)
                    val selfVar = generateSelectionVar("Not")

                    // 0 ≤ 1 - child − self ≤ 0
                    val expr = GRBLinExpr()
                    expr.addConstant(1.0)
                    expr.addTerm(-1.0, childVar)
                    expr.addTerm(-1.0, selfVar)

                    model.addConstr(0.0, GRB.EQUAL, expr, generateConstraintName())

                    constraintMap[constr] = selfVar
                    selfVar
                }

                is VariableIn -> {
                    val selfVar = generateSelectionVar("variableIn")
                    if (assignmentVariableMap.contains(constr.variable)) {
                        assignmentVariableMap[constr.variable]!!.add(Pair(constr.protocol, selfVar))
                    } else {
                        assignmentVariableMap[constr.variable] = mutableListOf(Pair(constr.protocol, selfVar))
                    }

                    constraintMap[constr] = selfVar
                    selfVar
                }

                is HostVariable -> {
                    val selfVar = generateSelectionVar("hostVar")
                    constraintMap[constr] = selfVar
                    selfVar
                }

                is GuardVisibilityFlag -> {
                    val selfVar = generateSelectionVar("guardVisibilityFlag")
                    constraintMap[constr] = selfVar
                    selfVar
                }

                // generate new variable for this constraint,
                // and then later add extra constraints to encode its meaning (see solveSelectionProblem)
                is VariableEquals -> {
                    val selfVar = generateSelectionVar("variableEquals")
                    constraintMap[constr] = selfVar
                    variableEqualsSet.add(constr)
                    selfVar
                }

                is Literal -> {
                    val selfVar = generateSelectionVar("literal")
                    constraintMap[constr] = selfVar
                    model.addConstr(if (constr.literalValue) 1.0 else 0.0, GRB.EQUAL, selfVar, generateConstraintName())
                    selfVar
                }

                False -> translateSelectionConstraint(Literal(false))

                True -> translateSelectionConstraint(Literal(true))
            }
    }

    // similar to translating the selection constr, but for the costs
    private fun translateCostConstraint(symCost: SymbolicCost, bigM: Int): GRBVar {
        return costMap[symCost]
            ?: when (symCost) {
                is CostAdd -> {
                    val lhsVar = translateCostConstraint(symCost.lhs, bigM)
                    val rhsVar = translateCostConstraint(symCost.rhs, bigM)
                    val selfVar = generateCostVar("CostAdd")

                    val expr = GRBLinExpr()
                    expr.addTerm(1.0, selfVar)
                    expr.addTerm(-1.0, lhsVar)
                    expr.addTerm(-1.0, rhsVar)

                    model.addConstr(0.0, GRB.LESS_EQUAL, expr, generateConstraintName())
                    selfVar
                }

                is CostMul -> {
                    val rhsVar = translateCostConstraint(symCost.rhs, bigM)
                    val selfVar = generateCostVar("CostMul")

                    val expr = GRBLinExpr()
                    expr.addTerm(1.0, selfVar)
                    expr.addTerm((-symCost.lhs).toDouble(), rhsVar)

                    model.addConstr(0.0, GRB.LESS_EQUAL, expr, generateConstraintName())

                    selfVar
                }

                is CostMax -> {
                    val lhsVar = translateCostConstraint(symCost.lhs, bigM)
                    val rhsVar = translateCostConstraint(symCost.rhs, bigM)
                    val selfVar = generateCostVar("CostMax")

                    model.addGenConstrMax(selfVar, arrayOf(lhsVar, rhsVar), 0.0, generateConstraintName())

                    selfVar
                }

                is CostChoice -> {
                    val lhsVars = mutableListOf<GRBVar>()
                    val selfVar = generateCostVar("CostChoice")
                    for (choice in symCost.choices) {
                        val lhsVar = translateSelectionConstraint(choice.first)
                        lhsVars.add(lhsVar)

                        val rhsVar = translateCostConstraint(choice.second, bigM)

                        val lhsExpr = GRBLinExpr()
                        lhsExpr.addConstant(bigM.toDouble())
                        lhsExpr.addTerm(-bigM.toDouble(), lhsVar)

                        val rhsExpr = GRBLinExpr()
                        rhsExpr.addTerm(1.0, rhsVar)
                        rhsExpr.addTerm(-1.0, selfVar)

                        model.addConstr(lhsExpr, GRB.GREATER_EQUAL, rhsExpr, generateConstraintName())
                    }

                    // make sure exactly one of the choices are true
                    exclusivelyTrue(lhsVars, model)

                    selfVar
                }

                is CostLiteral -> {
                    val selfVar = generateCostVar("CostLiteral")
                    val litDouble = symCost.cost.toDouble()

                    model.addConstr(selfVar, GRB.EQUAL, litDouble, generateConstraintName())
                    selfVar
                }
            }
    }

    private fun computeBigMLowerBound(cost: SymbolicCost): Int {
        return when (cost) {
            is CostLiteral -> cost.cost
            is CostAdd -> computeBigMLowerBound(cost.lhs) + computeBigMLowerBound(cost.rhs)
            is CostMul -> cost.lhs * computeBigMLowerBound(cost.rhs)
            is CostMax -> max(computeBigMLowerBound(cost.lhs), computeBigMLowerBound(cost.rhs))
            is CostChoice -> Collections.max(cost.choices.map { c -> computeBigMLowerBound(c.second) })
        }
    }

    // given a set of propositions, generate constraint to ensure that exactly one of them is true
    private fun exclusivelyTrue(vars: List<GRBVar>, model: GRBModel) {
        val sumExpr = GRBLinExpr()
        for (v in vars) {
            sumExpr.addTerm(1.0, v)
        }

        model.addConstr(1.0, GRB.GREATER_EQUAL, sumExpr, generateConstraintName())
        model.addConstr(sumExpr, GRB.GREATER_EQUAL, 1.0, generateConstraintName())
    }

    override fun solveSelectionProblem(problem: SelectionProblem): ProtocolAssignment? {
        val bigM = computeBigMLowerBound(problem.cost)

        // translation: translate constraints into Gurobi constraints
        for (constraint in problem.constraints) {
            translateSelectionConstraint(constraint)
        }

        if (assignmentVariableMap.keys.isEmpty()) {
            return ProtocolAssignment(mapOf(), mapOf(), problem)
        }

        // add extra constraints to ensure exactly one protocol is selected per assignment variable
        for (assignmentVarMappings in assignmentVariableMap) {
            exclusivelyTrue(assignmentVarMappings.value.map { pair -> pair.second }, model)
        }

        // add extra constraints to encode meaning of VariableEquals constraints
        for (variableEquals in variableEqualsSet) {
            val fv1 = variableEquals.var1
            val fv2 = variableEquals.var2

            // given common protocols p1, ..., pn for fv1 and fv2 where fv_i = p_j is represented by v_ij,
            // VariableEquals(fv1, fv2) <=> ((v_11 && v_21) || (v_12 && v_22) || ... || (v_1n && v_2n))
            translateSelectionConstraint(
                iff(
                    variableEquals,
                    Or(
                        assignmentVariableMap[fv1]!!
                            .map { pair -> pair.first }.toSet()
                            .intersect(
                                assignmentVariableMap[fv2]!!.map { pair -> pair.first }.toSet()
                            ).map { protocol ->
                                And(VariableIn(fv1, protocol), VariableIn(fv2, protocol))
                            }
                    )
                )
            )
        }

        val costVar = translateCostConstraint(problem.cost, bigM)
        val costExpr = GRBLinExpr()
        costExpr.addTerm(1.0, costVar)

        model.setObjective(costExpr, GRB.MINIMIZE)
        model.optimize()

        val numSolutions = model.get(GRB.IntAttr.SolCount)
        val solution: Map<FunctionVariable, Protocol>?
        if (numSolutions > 0) {
            solution = mutableMapOf()
            for (assignmentVarMappings in assignmentVariableMap) {
                val assignmentVar = assignmentVarMappings.key
                val protocolVarPairs = assignmentVarMappings.value
                for (protocolVarPair in protocolVarPairs) {
                    val varValue = protocolVarPair.second.get(GRB.DoubleAttr.X)
                    if (varValue != 0.0) {
                        if (!solution.containsKey(assignmentVar)) {
                            solution[assignmentVar] = protocolVarPair.first
                        } else throw Error("multiple protocols assigned for $assignmentVar")
                    }
                }
            }
            return ProtocolAssignment(solution, mapOf(), problem)
        } else {
            return null
        }
    }
}
