package edu.cornell.cs.apl.viaduct.security.solver2

import edu.cornell.cs.apl.viaduct.algebra.BoundedLattice
import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra
import edu.cornell.cs.apl.viaduct.security.SecurityLattice
import java.io.Writer
import edu.cornell.cs.apl.viaduct.algebra.solver2.ConstraintSystem as ComponentSystem

/**
 * Given a set of [SecurityLattice] constraints, finds an assignment to all variables that
 * minimizes the trust assigned to each variable (if one exists).
 *
 * @param T type of exceptions thrown when there are unsatisfiable constraints.
 */
class ConstraintSystem<C : HeytingAlgebra<C>, V, T : Throwable>(
    constraints: Iterable<Constraint<C, V, T>>,
    bounds: BoundedLattice<C>
) {
    private val componentSystem = ComponentSystem(constraints, bounds)

    /**
     * Returns the least-trust solution to the set of constraints in the system.
     *
     * @throws T if there are unsatisfiable constraints.
     */
    fun solution(): ConstraintSolution<C, V> = ConstraintSolution(componentSystem.solution())

    /** Outputs the constraint system as a DOT graph to [writer]. */
    fun exportDotGraph(writer: Writer) {
        componentSystem.exportDotGraph(writer)
    }
}
