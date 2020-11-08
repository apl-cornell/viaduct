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
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.createdVariables
import edu.cornell.cs.apl.viaduct.analysis.involvedVariables
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode

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

data class HostVariable(val variable: BoolExpr) : SelectionConstraint() {
    override val asDocument: Document = Document("$variable")
}

data class GuardVisibilityFlag(val variable: BoolExpr) : SelectionConstraint() {
    override val asDocument: Document = Document("$variable")
}

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

        // TODO: ignore host variables for now
        is HostVariable -> true

        // TODO: ignore guard visibility flags for now
        is GuardVisibilityFlag -> true
    }
}

internal fun List<SelectionConstraint>.assert(
    context: Set<SelectionConstraint>,
    f: (FunctionName, Variable) -> Protocol
) {
    for (c in this) {
        if (c is And) {
            listOf(c.lhs, c.rhs).assert(context, f)
        } else if (c is Implies) {
            if (c.lhs.evaluate(f)) {
                listOf(c.rhs).assert(context + setOf(c.lhs), f)
            }
        } else if (!c.evaluate(f)) {
            assert(false)
        }
    }
}

internal fun SelectionConstraint.or(other: SelectionConstraint): SelectionConstraint {
    return Or(this, other)
}

internal fun SelectionConstraint.implies(other: SelectionConstraint): SelectionConstraint {
    return Implies(this, other)
}

internal fun SymbolicCost.evaluate(
    f: (FunctionName, Variable) -> Protocol,
    c: (CostVariable) -> Int
): Int {
    return when (this) {
        is CostLiteral -> this.cost
        is CostVariable -> c(this)
        is CostAdd -> this.lhs.evaluate(f, c) + this.rhs.evaluate(f, c)
        is CostMul -> this.lhs.evaluate(f, c) * this.rhs.evaluate(f, c)
        is CostMux -> if (this.guard.evaluate(f)) this.lhs.evaluate(f, c) else this.rhs.evaluate(f, c)
    }
}

internal fun List<BoolExpr>.ors(ctx: Context): BoolExpr {
    return ctx.mkOr(* this.toTypedArray())
}

internal fun List<SelectionConstraint>.ors(): SelectionConstraint {
    return this.fold(Literal(false) as SelectionConstraint) { acc, x -> Or(acc, x) }
}

internal fun List<SelectionConstraint>.ands(): SelectionConstraint {
    return this.fold(Literal(true) as SelectionConstraint) { acc, x -> And(acc, x) }
}

internal fun iff(lhs: SelectionConstraint, rhs: SelectionConstraint): SelectionConstraint =
    And(Implies(lhs, rhs), Implies(rhs, lhs))

/** Convert a SelectionConstraint into a Z3 BoolExpr. **/
internal fun SelectionConstraint.boolExpr(
    ctx: Context,
    vmap: BiMap<FunctionVariable, IntExpr>,
    pmap: BiMap<Protocol, IntExpr>
): BoolExpr {
    return when (this) {
        is HostVariable -> this.variable
        is GuardVisibilityFlag -> this.variable
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

/** Some convenience functions. **/

fun SymbolicCost.costVariables(): Set<CostVariable> =
    when (this) {
        is CostLiteral -> setOf()
        is CostVariable -> setOf(this)
        is CostAdd -> this.lhs.costVariables().union(this.rhs.costVariables())
        is CostMul -> this.lhs.costVariables().union(this.rhs.costVariables())
        is CostMux ->
            this.guard.costVariables().union(
                this.lhs.costVariables()
                    .union(this.rhs.costVariables())
            )
    }

fun SelectionConstraint.costVariables(): Set<CostVariable> =
    when (this) {
        is HostVariable -> setOf()
        is GuardVisibilityFlag -> setOf()
        is Literal -> setOf()
        is Implies -> this.lhs.costVariables().union(this.rhs.costVariables())
        is Or -> this.lhs.costVariables().union(this.rhs.costVariables())
        is VariableIn -> setOf()
        is VariableEquals -> setOf()
        is CostEquals -> this.lhs.costVariables().union(this.rhs.costVariables())
        is Not -> this.rhs.costVariables()
        is And -> this.lhs.costVariables().union(this.rhs.costVariables())
    }

fun SelectionConstraint.hostVariables(): Set<HostVariable> =
    when (this) {
        is HostVariable -> setOf(this)
        is GuardVisibilityFlag -> setOf()
        is Literal -> setOf()
        is Implies -> this.lhs.hostVariables().union(this.rhs.hostVariables())
        is Or -> this.lhs.hostVariables().union(this.rhs.hostVariables())
        is VariableIn -> setOf()
        is VariableEquals -> setOf()
        is CostEquals -> setOf()
        is Not -> this.rhs.hostVariables()
        is And -> this.lhs.hostVariables().union(this.rhs.hostVariables())
    }

fun SelectionConstraint.guardVisibilityVariables(): Set<GuardVisibilityFlag> =
    when (this) {
        is HostVariable -> setOf()
        is GuardVisibilityFlag -> setOf(this)
        is Literal -> setOf()
        is Implies -> this.lhs.guardVisibilityVariables().union(this.rhs.guardVisibilityVariables())
        is Or -> this.lhs.guardVisibilityVariables().union(this.rhs.guardVisibilityVariables())
        is VariableIn -> setOf()
        is VariableEquals -> setOf()
        is CostEquals -> setOf()
        is Not -> this.rhs.guardVisibilityVariables()
        is And -> this.lhs.guardVisibilityVariables().union(this.rhs.guardVisibilityVariables())
    }

/** States whether an expression reads only from the protocols in [prots] **/
fun ExpressionNode.readsFrom(nameAnalysis: NameAnalysis, prots: Set<Protocol>): SelectionConstraint =
    this.involvedVariables().map {
        VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(this), it), prots)
    }.ands()

/** States that if the let node is stored at any protocol in [to], it reads from only the protocols in [from] **/
fun LetNode.readsFrom(nameAnalysis: NameAnalysis, to: Set<Protocol>, from: Set<Protocol>): SelectionConstraint =
    Implies(
        VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value), to),
        this.value.readsFrom(nameAnalysis, from)
    )

fun DeclarationNode.readsFrom(nameAnalysis: NameAnalysis, to: Set<Protocol>, from: Set<Protocol>): SelectionConstraint =
    Implies(
        VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value), to),
        this.arguments.map { it.readsFrom(nameAnalysis, from) }.ands()
    )

/** States that if the let node is stores at any protocol in [from], it sends to only the protocols in [to] **/

fun LetNode.sendsTo(nameAnalysis: NameAnalysis, from: Set<Protocol>, to: Set<Protocol>): SelectionConstraint =
    Implies(
        VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value), from),
        nameAnalysis.readers(this).map { stmt ->
            stmt.createdVariables().map {
                VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(stmt), it), to)
            }.ands()
        }.ands()
    )

fun DeclarationNode.sendsTo(nameAnalysis: NameAnalysis, from: Set<Protocol>, to: Set<Protocol>): SelectionConstraint =
    Implies(
        VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value), from),
        nameAnalysis.queriers(this).map {
            val clet = nameAnalysis.correspondingLet(it)
            VariableIn(FunctionVariable(nameAnalysis.enclosingFunctionName(clet), clet.temporary.value), to)
        }.ands()
    )
