package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.createdVariables
import edu.cornell.cs.apl.viaduct.analysis.involvedVariables
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node

data class FunctionVariable(val function: FunctionName, val variable: Variable) : PrettyPrintable {
    override fun toDocument(): Document =
        Document("(") + function + "," + variable.toDocument() + Document(")")
}

/** Symbolic cost that will be minimized by a solver. */
sealed class SymbolicCost : CostMonoid<SymbolicCost> {
    companion object {
        fun zero(): SymbolicCost = CostLiteral(0)
    }

    override fun concat(other: SymbolicCost): SymbolicCost =
        CostAdd(this, other)

    override fun zero(): SymbolicCost = SymbolicCost.zero()
}

data class CostLiteral(val cost: Int) : SymbolicCost() {
    override fun toDocument(): Document = Document(cost.toString())
}

data class CostAdd(val lhs: SymbolicCost, val rhs: SymbolicCost) : SymbolicCost() {
    override fun toDocument(): Document = lhs.toDocument() * Document("+") * rhs.toDocument()
}

/** Multiply cost expression with a scalar. Restrict to scalar multiplication to keep constraint problem linear. */
data class CostMul(val lhs: Int, val rhs: SymbolicCost) : SymbolicCost() {
    override fun toDocument(): Document = Document(lhs.toString()) * Document("*") * rhs.toDocument()
}

data class CostMax(val lhs: SymbolicCost, val rhs: SymbolicCost) : SymbolicCost() {
    override fun toDocument(): Document = Document("max") * listOf(lhs.toDocument(), rhs.toDocument()).tupled()
}

/** Cost determined by which guard is true. Exactly one guard must be true. */
data class CostChoice(val choices: List<Pair<SelectionConstraint, SymbolicCost>>) : SymbolicCost() {
    override fun toDocument(): Document =
        this.choices.map { choice ->
            choice.first.toDocument() * Document("=>") * choice.second.toDocument()
        }.tupled()
}

/** Custom selection constraints specified for constraint solving during splitting. */
sealed class SelectionConstraint : PrettyPrintable

object True : SelectionConstraint() {
    override fun toDocument(): Document = Document("true")
}

object False : SelectionConstraint() {
    override fun toDocument(): Document = Document("false")
}

data class HostVariable(val variable: String) : SelectionConstraint() {
    override fun toDocument(): Document = Document(variable)
}

data class GuardVisibilityFlag(val variable: String) : SelectionConstraint() {
    override fun toDocument(): Document = Document(variable)
}

data class Literal(val literalValue: Boolean) : SelectionConstraint() {
    override fun toDocument(): Document = Document(literalValue.toString())
}

data class Implies(val lhs: SelectionConstraint, val rhs: SelectionConstraint) : SelectionConstraint() {
    override fun toDocument(): Document = lhs.toDocument() * Document("=>") * rhs.toDocument()
}

data class Or(val props: List<SelectionConstraint>) : SelectionConstraint() {
    constructor(vararg props: SelectionConstraint) : this(listOf(*props))

    override fun toDocument(): Document =
        when (props.size) {
            0 -> Document("")
            1 -> props.first().toDocument()
            else -> {
                props.subList(1, props.size - 1).fold(props.first().toDocument()) { acc, prop ->
                    acc * Document("||") * prop.toDocument()
                }
            }
        }
}

data class And(val props: List<SelectionConstraint>) : SelectionConstraint() {
    constructor(vararg props: SelectionConstraint) : this(listOf(*props))

    override fun toDocument(): Document =
        when (props.size) {
            0 -> Document("")
            1 -> props.first().toDocument()
            else -> {
                props.subList(1, props.size - 1).fold(props.first().toDocument()) { acc, prop ->
                    acc * Document("&&") * prop.toDocument()
                }
            }
        }
}

data class Not(val rhs: SelectionConstraint) : SelectionConstraint() {
    override fun toDocument(): Document = Document("!") + rhs.toDocument()
}

/** VariableIn(v, P) holds when v is selected to be a protocol in P **/
data class VariableIn(val variable: FunctionVariable, val protocol: Protocol) : SelectionConstraint() {
    override fun toDocument(): Document =
        variable * Document("=") * protocol.toDocument()
}

/** Protocols for v1 and v2 are equal. */
data class VariableEquals(val var1: FunctionVariable, val var2: FunctionVariable) : SelectionConstraint() {
    override fun toDocument(): Document = var1.toDocument() * Document("==") * var2.toDocument()
}

/** A constrained optimization problem defined by a set of selection constraints
 * and a cost expression to minimize. */
data class SelectionProblem(
    /** Set of constraints that must hold true for any valid protocol assignment. */
    val constraints: Set<SelectionConstraint>,

    /** Cost for the whole program. */
    val cost: SymbolicCost,

    /** Extra metadata that lets us associate cost with different parts of a program. */
    val costMap: Map<Node, SymbolicCost> = mapOf()
)

internal fun SelectionConstraint.or(other: SelectionConstraint): SelectionConstraint =
    Or(listOf(this, other))

internal fun SelectionConstraint.implies(other: SelectionConstraint): SelectionConstraint =
    Implies(this, other)

internal fun variableInSet(fv: FunctionVariable, protocols: Set<Protocol>): SelectionConstraint =
    protocols.map { protocol -> VariableIn(fv, protocol) }.ors()

internal fun List<SelectionConstraint>.ors(): SelectionConstraint = Or(this)

internal fun List<SelectionConstraint>.ands(): SelectionConstraint = And(this)

internal fun iff(lhs: SelectionConstraint, rhs: SelectionConstraint): SelectionConstraint =
    And(Implies(lhs, rhs), Implies(rhs, lhs))

/** Some convenience functions. **/

fun SelectionConstraint.functionVariables(): Set<FunctionVariable> =
    when (this) {
        is True -> setOf()
        is False -> setOf()
        is HostVariable -> setOf()
        is GuardVisibilityFlag -> setOf()
        is Literal -> setOf()
        is Implies -> this.lhs.functionVariables().union(this.rhs.functionVariables())
        is Or -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.functionVariables()) }
        is And -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.functionVariables()) }
        is Not -> this.rhs.functionVariables()
        is VariableIn -> setOf(this.variable)
        is VariableEquals -> setOf(this.var1, this.var2)
    }

fun SelectionConstraint.protocols(): Set<Protocol> =
    when (this) {
        is True -> setOf()
        is False -> setOf()
        is HostVariable -> setOf()
        is GuardVisibilityFlag -> setOf()
        is Literal -> setOf()
        is Implies -> this.lhs.protocols().union(this.rhs.protocols())
        is Or -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.protocols()) }
        is And -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.protocols()) }
        is Not -> this.rhs.protocols()
        is VariableIn -> setOf(this.protocol)
        is VariableEquals -> setOf()
    }

fun SelectionConstraint.hostVariables(): Set<HostVariable> =
    when (this) {
        is True -> setOf()
        is False -> setOf()
        is HostVariable -> setOf(this)
        is GuardVisibilityFlag -> setOf()
        is Literal -> setOf()
        is Implies -> this.lhs.hostVariables().union(this.rhs.hostVariables())
        is Or -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.hostVariables()) }
        is And -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.hostVariables()) }
        is Not -> this.rhs.hostVariables()
        is VariableIn -> setOf()
        is VariableEquals -> setOf()
    }

fun SelectionConstraint.guardVisibilityVariables(): Set<GuardVisibilityFlag> =
    when (this) {
        is True -> setOf()
        is False -> setOf()
        is HostVariable -> setOf()
        is GuardVisibilityFlag -> setOf(this)
        is Literal -> setOf()
        is Implies -> this.lhs.guardVisibilityVariables().union(this.rhs.guardVisibilityVariables())
        is Or -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.guardVisibilityVariables()) }
        is And -> this.props.fold(setOf()) { acc, prop -> acc.union(prop.guardVisibilityVariables()) }
        is Not -> this.rhs.guardVisibilityVariables()
        is VariableIn -> setOf()
        is VariableEquals -> setOf()
    }

fun SelectionConstraint.variableNames(): Set<String> =
    this.hostVariables().map { hv -> hv.variable }.toSet().union(
        this.guardVisibilityVariables().map { gv -> gv.variable }
    )

/** States whether an expression reads only from the protocols in [prots] **/
fun ExpressionNode.readsFrom(nameAnalysis: NameAnalysis, prots: Set<Protocol>): SelectionConstraint =
    this.involvedVariables().map {
        variableInSet(FunctionVariable(nameAnalysis.enclosingFunctionName(this), it), prots)
    }.ands()

/** States that if the let node is stored at any protocol in [to], it reads from only the protocols in [from] **/
fun LetNode.readsFrom(nameAnalysis: NameAnalysis, to: Set<Protocol>, from: Set<Protocol>): SelectionConstraint =
    Implies(
        variableInSet(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value), to),
        this.value.readsFrom(nameAnalysis, from)
    )

fun DeclarationNode.readsFrom(nameAnalysis: NameAnalysis, to: Set<Protocol>, from: Set<Protocol>): SelectionConstraint =
    Implies(
        variableInSet(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value), to),
        this.arguments.map { it.readsFrom(nameAnalysis, from) }.ands()
    )

/** States that if the let node is stores at any protocol in [from], it sends to only the protocols in [to] **/

fun LetNode.sendsTo(nameAnalysis: NameAnalysis, from: Set<Protocol>, to: Set<Protocol>): SelectionConstraint =
    Implies(
        variableInSet(FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.temporary.value), from),
        nameAnalysis.readers(this).map { stmt ->
            stmt.createdVariables().map {
                variableInSet(FunctionVariable(nameAnalysis.enclosingFunctionName(stmt), it), to)
            }.ands()
        }.ands()
    )

fun DeclarationNode.sendsTo(nameAnalysis: NameAnalysis, from: Set<Protocol>, to: Set<Protocol>): SelectionConstraint =
    Implies(
        variableInSet(
            FunctionVariable(nameAnalysis.enclosingFunctionName(this), this.name.value),
            from
        ),
        nameAnalysis.queriers(this).map {
            val clet = nameAnalysis.correspondingLet(it)
            variableInSet(FunctionVariable(nameAnalysis.enclosingFunctionName(clet), clet.temporary.value), to)
        }.ands()
    )
