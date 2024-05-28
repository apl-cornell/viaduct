package io.github.aplcornell.viaduct.security.solver

import io.github.aplcornell.viaduct.algebra.BoundedLattice
import io.github.aplcornell.viaduct.algebra.HeytingAlgebra
import io.github.aplcornell.viaduct.algebra.Lattice
import io.github.aplcornell.viaduct.algebra.solver.IllegalTermException
import io.github.aplcornell.viaduct.algebra.solver.flowsTo
import io.github.aplcornell.viaduct.security.SecurityLattice
import io.github.aplcornell.viaduct.algebra.solver.Term as ComponentTerm

/** Represents a [SecurityLattice] expression that can appear in [Constraint]s. */
typealias Term<C, V> = SecurityLattice<ComponentTerm<C, ComponentVariable<V>>>

/** Embeds [constant] as a [Term]. */
fun <C : Lattice<C>, V> term(constant: SecurityLattice<C>): Term<C, V> =
    SecurityLattice(
        ComponentTerm.constant(constant.confidentialityComponent),
        ComponentTerm.constant(constant.integrityComponent),
    )

/** Embeds [variable] as a [Term]. */
fun <C : Lattice<C>, V> term(variable: V): Term<C, V> =
    SecurityLattice(
        ComponentTerm.variable(ConfidentialityVariable(variable)),
        ComponentTerm.variable(IntegrityVariable(variable)),
    )

/** A constraint variable for the confidentiality or the integrity component of a [SecurityLattice] element. */
sealed class ComponentVariable<V> {
    protected abstract val baseVariable: V
}

/** A constraint variable for [SecurityLattice.confidentialityComponent]. */
internal data class ConfidentialityVariable<V>(override val baseVariable: V) : ComponentVariable<V>() {
    override fun toString(): String = "$baseVariable→"
}

/** A constraint variable for [SecurityLattice.integrityComponent]. */
internal data class IntegrityVariable<V>(override val baseVariable: V) : ComponentVariable<V>() {
    override fun toString(): String = "$baseVariable←"
}

/**
 * Returns a constraint that represents `[this] flowsTo [that]`.
 *
 * @param failWith a function that generates the exception to throw if the returned constraint is unsatisfiable.
 * The function will be given best-effort estimates for the values of [this] and [that].
 *
 * @throws IllegalTermException if [this] or [that] is not allowed to appear in constraints.
 */
fun <C : HeytingAlgebra<C>, V, T> Term<C, V>.flowsTo(
    that: Term<C, V>,
    // TODO: it would be nicer if this didn't need bounds...
    bounds: BoundedLattice<C>,
    failWith: (SecurityLattice<C>, SecurityLattice<C>) -> T,
): Iterable<Constraint<C, V, T>> =
    confidentialityFlowsTo(that, bounds, failWith) +
        integrityFlowsTo(that, bounds, failWith)

fun <C : HeytingAlgebra<C>, V, T> Term<C, V>.integrityFlowsTo(
    that: Term<C, V>,
    bounds: BoundedLattice<C>,
    failWith: (SecurityLattice<C>, SecurityLattice<C>) -> T,
): Iterable<Constraint<C, V, T>> =
    listOf(
        this.integrityComponent.flowsTo(that.integrityComponent) { from, to ->
            failWith(
                SecurityLattice(from).integrity(bounds),
                SecurityLattice(to).integrity(bounds),
            )
        },
    )

fun <C : HeytingAlgebra<C>, V, T> Term<C, V>.confidentialityFlowsTo(
    that: Term<C, V>,
    bounds: BoundedLattice<C>,
    failWith: (SecurityLattice<C>, SecurityLattice<C>) -> T,
): Iterable<Constraint<C, V, T>> =
    listOf(
        that.confidentialityComponent.flowsTo(this.confidentialityComponent) { to, from ->
            failWith(
                SecurityLattice(from).confidentiality(bounds),
                SecurityLattice(to).confidentiality(bounds),
            )
        },
    )
