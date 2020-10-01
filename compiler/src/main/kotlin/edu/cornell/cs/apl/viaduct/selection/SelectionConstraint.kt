package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.ArithExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import com.uchuhimo.collections.BiMap
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.braced
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable

data class FunctionVariable(val function: FunctionName, val variable: Variable) : PrettyPrintable {
    override val asDocument: Document =
        Document("(") + function + "," + variable.asDocument + Document(")")
}

sealed class SymbolicCost : CostMonoid<SymbolicCost> {
    companion object {
        fun zero(): SymbolicCost = CostLiteral(0)
    }

    override fun concat(other: SymbolicCost): SymbolicCost =
        CostAdd(this, other)

    override fun zero(): SymbolicCost = SymbolicCost.zero()
}

data class CostLiteral(val cost: Int) : SymbolicCost() {
    override val asDocument: Document = Document(cost.toString())
}

data class CostVariable(val variable: IntExpr) : SymbolicCost() {
    override val asDocument: Document = Document(variable.toString())
}

data class CostAdd(val lhs: SymbolicCost, val rhs: SymbolicCost) : SymbolicCost() {
    override val asDocument: Document = lhs.asDocument * Document("+") * rhs.asDocument
}

data class CostMul(val lhs: SymbolicCost, val rhs: SymbolicCost) : SymbolicCost() {
    override val asDocument: Document = lhs.asDocument * Document("*") * rhs.asDocument
}

data class CostMux(val guard: SelectionConstraint, val lhs: SymbolicCost, val rhs: SymbolicCost) : SymbolicCost() {
    override val asDocument: Document =
        guard.asDocument * Document("?") * lhs.asDocument * Document(":") * rhs.asDocument
}

/** Custom selection constraints specified for constraint solving during splitting. */
sealed class SelectionConstraint : PrettyPrintable

data class Literal(val literalValue: Boolean) : SelectionConstraint() {
    override val asDocument: Document = Document(literalValue.toString())
}

data class Implies(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint() {
    override val asDocument: Document = lhs.asDocument * Document("=>") * rhs.asDocument
}

data class Or(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint() {
    override val asDocument: Document = lhs.asDocument * Document("||") * rhs.asDocument
}

/** VariableIn(v, P) holds when v is selected to be a protocol in P **/
data class VariableIn(val variable: FunctionVariable, val protocols: Set<Protocol>) : SelectionConstraint() {
    override val asDocument: Document =
        variable * Document("in") * protocols.map { it.asDocument }.braced()
}

/** Protocols for v1 and v2 are equal. */
data class VariableEquals(val var1: FunctionVariable, val var2: FunctionVariable) : SelectionConstraint() {
    override val asDocument: Document = var1.asDocument * Document("==") * var2.asDocument
}

data class CostEquals(val lhs: SymbolicCost, val rhs: SymbolicCost) : SelectionConstraint() {
    override val asDocument: Document = lhs.asDocument * Document("=") * rhs.asDocument
}

data class Not(val rhs: SelectionConstraint) : SelectionConstraint() {
    override val asDocument: Document = Document("!") + rhs.asDocument
}

data class And(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint() {
    override val asDocument: Document = lhs.asDocument * Document("&&") * rhs.asDocument
}

internal fun Boolean.implies(r: Boolean) = (!this) || r

/** Given a protocol selection, evaluate the constraints. **/
internal fun SelectionConstraint.evaluate(f: (FunctionName, Variable) -> Protocol): Boolean {
    return when (this) {
        is Literal -> literalValue
        is Implies -> lhs.evaluate(f).implies(rhs.evaluate(f))
        is Or -> (lhs.evaluate(f)) || (rhs.evaluate(f))
        is And -> (lhs.evaluate(f)) && (rhs.evaluate(f))
        is VariableIn -> protocols.contains(f(variable.function, variable.variable))
        is Not -> !(rhs.evaluate(f))
        is VariableEquals -> f(var1.function, var1.variable) == f(var2.function, var2.variable)

        // TODO: ignore cost constraints for now
        is CostEquals -> true
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
    vmap: BiMap<FunctionVariable, IntExpr>,
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

        is CostEquals ->
            ctx.mkEq(
                this.lhs.arithExpr(ctx, vmap, pmap),
                this.rhs.arithExpr(ctx, vmap, pmap)
            )
    }
}

/** Convert a CostExpression into a Z3 ArithExpr. */
internal fun SymbolicCost.arithExpr(
    ctx: Context,
    vmap: BiMap<FunctionVariable, IntExpr>,
    pmap: BiMap<Protocol, IntExpr>
): ArithExpr =
    when (this) {
        is CostLiteral -> ctx.mkInt(this.cost)
        is CostVariable -> this.variable

        is CostAdd ->
            ctx.mkAdd(
                this.lhs.arithExpr(ctx, vmap, pmap),
                this.rhs.arithExpr(ctx, vmap, pmap)
            )

        is CostMul ->
            ctx.mkMul(
                this.lhs.arithExpr(ctx, vmap, pmap),
                this.rhs.arithExpr(ctx, vmap, pmap)
            )

        is CostMux ->
            ctx.mkITE(
                this.guard.boolExpr(ctx, vmap, pmap),
                this.lhs.arithExpr(ctx, vmap, pmap),
                this.rhs.arithExpr(ctx, vmap, pmap)
            ) as ArithExpr
    }
