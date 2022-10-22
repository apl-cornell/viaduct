package io.github.apl_cornell.viaduct.security.solver2

import io.github.apl_cornell.viaduct.security.SecurityLattice
import io.github.apl_cornell.viaduct.algebra.solver2.Constraint as ComponentConstraint

/** A [SecurityLattice] constraint. */
typealias Constraint<C, V, T> = ComponentConstraint<C, ComponentVariable<V>, T>
