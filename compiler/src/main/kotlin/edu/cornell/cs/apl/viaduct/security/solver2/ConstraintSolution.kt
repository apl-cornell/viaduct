package edu.cornell.cs.apl.viaduct.security.solver2

import edu.cornell.cs.apl.viaduct.algebra.Lattice
import edu.cornell.cs.apl.viaduct.security.SecurityLattice
import edu.cornell.cs.apl.viaduct.algebra.solver2.ConstraintSolution as ComponentSolution

/** A solution to a system of constraints. Maps variables to values. */
class ConstraintSolution<C : Lattice<C>, V> internal constructor(
    private val componentSolution: ComponentSolution<C, ComponentVariable<V>>
) : (V) -> SecurityLattice<C> {
    /** Returns the value of [term]. */
    fun evaluate(term: Term<C, V>): SecurityLattice<C> =
        SecurityLattice(
            componentSolution.evaluate(term.confidentialityComponent),
            componentSolution.evaluate(term.integrityComponent)
        )

    override fun invoke(variable: V): SecurityLattice<C> =
        evaluate(term(variable))
}
