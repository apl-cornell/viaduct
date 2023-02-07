package io.github.aplcornell.viaduct.security.solver

import io.github.aplcornell.viaduct.security.SecurityLattice
import io.github.aplcornell.viaduct.algebra.solver.Constraint as ComponentConstraint

/** A [SecurityLattice] constraint. */
typealias Constraint<C, V, T> = ComponentConstraint<C, ComponentVariable<V>, T>
