package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import com.uchuhimo.collections.BiMap
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable

/** Custom selection constraints specified for constraint solving during splitting. */
sealed class SelectionConstraint

data class Literal(val literalValue: Boolean) : SelectionConstraint()
data class Implies(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint()
data class Or(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint()

/** VariableIn(v, P) holds when v is selected to be a protocol in P **/
data class VariableIn(val variable: Variable, val protocols: Set<Protocol>) : SelectionConstraint()

/** Protocols for v1 and v2 are equal. */
data class VariableEquals(val var1: Variable, val var2: Variable) : SelectionConstraint()

data class Not(val rhs: SelectionConstraint) : SelectionConstraint()
data class And(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint()

internal fun Boolean.implies(r: Boolean) = (!this) || r

/** Given a protocol selection, evaluate the constraints. **/
internal fun SelectionConstraint.evaluate(f: (Variable) -> Protocol): Boolean {
    return when (this) {
        is Literal -> literalValue
        is Implies -> lhs.evaluate(f).implies(rhs.evaluate(f))
        is Or -> (lhs.evaluate(f)) || (rhs.evaluate(f))
        is And -> (lhs.evaluate(f)) && (rhs.evaluate(f))
        is VariableIn -> protocols.contains(f(variable))
        is Not -> !(rhs.evaluate(f))
        is VariableEquals -> f(var1) == f(var2)
    }
}

internal fun List<BoolExpr>.ors(ctx: Context): BoolExpr {
    return ctx.mkOr(* this.toTypedArray())
}

internal fun List<SelectionConstraint>.ands(): SelectionConstraint {
    return this.fold(Literal(true) as SelectionConstraint) { acc, x -> And(acc, x) }
}

/** Convert a SelectionConstraint into a Z3 BoolExpr. **/
internal fun SelectionConstraint.boolExpr(
    ctx: Context,
    vmap: BiMap<Variable, IntExpr>,
    pmap: BiMap<Protocol, IntExpr>
): BoolExpr {
    return when (this) {
        is Literal -> ctx.mkBool(literalValue)
        is Implies -> ctx.mkImplies(lhs.boolExpr(ctx, vmap, pmap), rhs.boolExpr(ctx, vmap, pmap))
        is Or -> ctx.mkOr(lhs.boolExpr(ctx, vmap, pmap), rhs.boolExpr(ctx, vmap, pmap))
        is And -> ctx.mkAnd(lhs.boolExpr(ctx, vmap, pmap), rhs.boolExpr(ctx, vmap, pmap))
        is Not -> ctx.mkNot(rhs.boolExpr(ctx, vmap, pmap))
        is VariableIn ->
            this.protocols.map { prot ->
                ctx.mkEq(
                    vmap.get(this.variable),
                    pmap.get(prot)
                )
            }.ors(ctx)
        is VariableEquals -> ctx.mkEq(vmap.get(this.var1), vmap.get(this.var2))
    }
}
