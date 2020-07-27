package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable

sealed class SelectionConstraint

data class Literal(val literalValue: Boolean) : SelectionConstraint()
data class Implies(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint()
data class Or(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint()
data class VariableIn(val variable: Variable, val protocols: Set<Protocol>) : SelectionConstraint()
data class Not(val rhs: SelectionConstraint) : SelectionConstraint()
data class And(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint()

fun Boolean.implies(r: Boolean) = (!this) || r

fun SelectionConstraint.evaluate(f: (Variable) -> Protocol): Boolean {
    return when (this) {
        is Literal -> literalValue
        is Implies -> lhs.evaluate(f).implies(rhs.evaluate(f))
        is Or -> (lhs.evaluate(f)) || (rhs.evaluate(f))
        is And -> (lhs.evaluate(f)) && (rhs.evaluate(f))
        is VariableIn -> protocols.contains(f(variable))
        is Not -> !(rhs.evaluate(f))
    }
}
