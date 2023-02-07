package io.github.aplcornell.viaduct.algebra.solver

import io.github.aplcornell.viaduct.algebra.BoundedLattice
import io.github.aplcornell.viaduct.algebra.HeytingAlgebra
import io.github.aplcornell.viaduct.algebra.JoinSemiLattice
import io.github.aplcornell.viaduct.algebra.Lattice
import io.github.aplcornell.viaduct.algebra.MeetSemiLattice
import io.github.aplcornell.viaduct.util.dataflow.DataFlowNode

/** Represents a lattice expression that can appear in [Constraint]s. */
sealed class Term<C, V> : Lattice<Term<C, V>> {
    override fun join(that: Term<C, V>): Term<C, V> = Join(this, that)

    @JvmName("joinConstant")
    fun join(that: C): Term<C, V> = join(constant(that))

    @JvmName("joinVariable")
    fun join(that: V): Term<C, V> = join(variable(that))

    override fun meet(that: Term<C, V>): Term<C, V> = Meet(this, that)

    @JvmName("meetConstant")
    fun meet(that: C): Term<C, V> = meet(constant(that))

    @JvmName("meetVariable")
    fun meet(that: V): Term<C, V> = meet(variable(that))

    /** Provides bounds for a [Term] given bounds for [C]. */
    class Bounds<C : Lattice<C>, V>(bounds: BoundedLattice<C>) : BoundedLattice<Term<C, V>> {
        override val bottom: Term<C, V> =
            constant(bounds.bottom)

        override val top: Term<C, V> =
            constant(bounds.top)
    }

    companion object {
        fun <C, V> constant(value: C): Term<C, V> = Constant(value)

        fun <C, V> variable(value: V): Term<C, V> = Variable(value)
    }
}

/** A simple [Term] which can be a constant or a variable, but not a complex expression. */
internal sealed class AtomicTerm<C, V> : Term<C, V>(), DataFlowNode<C>

/** A term representing a constant element. */
internal data class Constant<C, V>(val value: C) : AtomicTerm<C, V>() {
    override fun transfer(input: C): C = value

    override fun toString(): String = value.toString()
}

/** A variable for the solver to find a value for. */
internal data class Variable<C, V>(val value: V) : AtomicTerm<C, V>() {
    override fun transfer(input: C): C = input

    override fun toString(): String = value.toString()
}

/** Represents the join of [lhs] and [rhs]. */
internal data class Join<C, V>(val lhs: Term<C, V>, val rhs: Term<C, V>) : Term<C, V>()

/** Represents the meet of [lhs] and [rhs]. */
internal data class Meet<C, V>(val lhs: Term<C, V>, val rhs: Term<C, V>) : Term<C, V>()

/**
 * Represents the join or the meet of at most one constant and at most one variable.
 * Whether it is a join or a meet is determined by context and by the function passed to [combine].
 */
private data class ConstantAndVariable<C, V>(val constant: Constant<C, V>?, val variable: Variable<C, V>?) {
    init {
        require(constant != null || variable != null)
    }

    constructor(term: AtomicTerm<C, V>) : this(term as? Constant, term as? Variable)

    /** @throws IllegalArgumentException if the result cannot be represented as a [ConstantAndVariable]. */
    fun combine(that: ConstantAndVariable<C, V>, operation: (lhs: C, rhs: C) -> C): ConstantAndVariable<C, V> {
        val c = merge(this.constant, that.constant) { c1, c2 -> Constant(operation(c1.value, c2.value)) }
        val v = merge(this.variable, that.variable, ::merge)
        return ConstantAndVariable(c, v)
    }

    companion object {
        /**
         * Returns whichever of [a1] and [a2] that is not `null`. Returns `null` if both are,
         * and combines the values with [both] if neither is.
         */
        private fun <A : Any> merge(a1: A?, a2: A?, both: (a1: A, a2: A) -> A): A? =
            when {
                a1 == null ->
                    a2

                a2 == null ->
                    a1

                else ->
                    both(a1, a2)
            }

        /**
         * Represents two variables as a single variable.
         *
         * @throws IllegalArgumentException if the result cannot be represented as a [Variable].
         */
        private fun <C, V> merge(v1: Variable<C, V>, v2: Variable<C, V>): Variable<C, V> =
            if (v1 == v2) v1 else throw IllegalArgumentException("Cannot merge ${v1.value} and ${v2.value}.")
    }
}

/**
 * Represents [term] as the join of many meets.
 *
 * @throws IllegalArgumentException if a meet cannot be represented as a [ConstantAndVariable].
 */
private fun <C : MeetSemiLattice<C>, V> joinOfMeets(term: Term<C, V>): List<ConstantAndVariable<C, V>> =
    when (term) {
        is AtomicTerm ->
            listOf(ConstantAndVariable(term))

        is Join ->
            joinOfMeets(term.lhs) + joinOfMeets(term.rhs)

        is Meet ->
            joinOfMeets(term.lhs).flatMap { m1 ->
                joinOfMeets(term.rhs).map { m2 ->
                    m1.combine(m2, MeetSemiLattice<C>::meet)
                }
            }
    }

/**
 * Represents [term] as the meet of many joins.
 *
 * @throws IllegalArgumentException if a join cannot be represented as a [ConstantAndVariable].
 */
private fun <C : JoinSemiLattice<C>, V> meetOfJoins(term: Term<C, V>): List<ConstantAndVariable<C, V>> =
    when (term) {
        is AtomicTerm ->
            listOf(ConstantAndVariable(term))

        is Join ->
            meetOfJoins(term.lhs).flatMap { j1 ->
                meetOfJoins(term.rhs).map { j2 ->
                    j1.combine(j2, JoinSemiLattice<C>::join)
                }
            }

        is Meet ->
            meetOfJoins(term.lhs) + meetOfJoins(term.rhs)
    }

/**
 * Returns a constraint that represents `[this] flowsTo [that]`.
 *
 * @param failWith a function that generates the exception to throw if the returned constraint is unsatisfiable.
 * The function will be given best-effort estimates for the values of [this] and [that].
 * @throws IllegalTermException if [this] or [that] is not allowed to appear in constraints.
 */
fun <C : HeytingAlgebra<C>, V, T> Term<C, V>.flowsTo(
    that: Term<C, V>,
    failWith: (C, C) -> T,
): Constraint<C, V, T> {
    fun node(term: ConstantAndVariable<C, V>): AtomicTerm<C, V> =
        term.variable ?: term.constant!!

    fun leftHandEdge(term: ConstantAndVariable<C, V>): LeftHandEdge<C> =
        if (term.constant != null && term.variable != null) {
            ImplyEdge(term.constant.value)
        } else {
            IdentityEdge()
        }

    fun rightHandEdge(term: ConstantAndVariable<C, V>): RightHandEdge<C> =
        if (term.constant != null && term.variable != null) {
            JoinEdge(term.constant.value)
        } else {
            IdentityEdge()
        }

    val lhs =
        try {
            joinOfMeets(this)
        } catch (e: IllegalArgumentException) {
            throw IllegalTermException(this)
        }

    val rhs =
        try {
            meetOfJoins(that)
        } catch (e: IllegalArgumentException) {
            throw IllegalTermException(that)
        }

    val reduced =
        lhs.flatMap { left ->
            rhs.map { right ->
                val edge = leftHandEdge(left).compose(rightHandEdge(right))
                ReducedFlowsToConstraint(node(left), edge, node(right))
            }
        }
    return FlowsToConstraint(this, that, failWith, reduced)
}
