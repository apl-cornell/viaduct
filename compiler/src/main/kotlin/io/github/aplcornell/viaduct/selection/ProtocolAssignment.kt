package io.github.aplcornell.viaduct.selection

import io.github.aplcornell.viaduct.syntax.FunctionName
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.Variable
import java.lang.Integer.max

data class ProtocolAssignment(
    val assignment: Map<FunctionVariable, Protocol>,
    val propModel: Map<String, Boolean>,
    val problem: SelectionProblem
) {
    fun getAssignment(fv: FunctionVariable): Protocol =
        assignment.getValue(fv)

    fun getAssignment(f: FunctionName, v: Variable): Protocol =
        getAssignment(FunctionVariable(f, v))

    /** Given a protocol selection, evaluate the constraints. **/
    fun evaluate(c: SelectionConstraint): Boolean =
        when (c) {
            is True -> true
            is False -> false
            is Literal -> c.literalValue
            is Implies -> (!evaluate(c.lhs)) || evaluate(c.rhs)
            is Or -> c.props.any { prop -> evaluate(prop) }
            is And -> c.props.all { prop -> evaluate(prop) }
            is Not -> !evaluate(c.rhs)
            is VariableIn -> getAssignment(c.variable) == c.protocol
            is VariableEquals -> getAssignment(c.var1) == getAssignment(c.var2)
            is HostVariable -> propModel[c.variable]!!
            is GuardVisibilityFlag -> propModel[c.variable]!!
        }

    fun evaluate(cost: SymbolicCost): Int =
        when (cost) {
            is CostLiteral -> cost.cost
            is CostAdd -> evaluate(cost.lhs) + evaluate(cost.rhs)
            is CostMul -> cost.lhs * evaluate(cost.rhs)
            is CostMax -> max(evaluate(cost.lhs), evaluate(cost.rhs))
            is CostChoice -> {
                var choiceCost: Int? = null
                for (choice in cost.choices) {
                    if (evaluate(choice.first)) {
                        choiceCost = evaluate(choice.second)
                    }
                }
                choiceCost ?: throw Error("at least one choice must be true")
            }
        }
}
