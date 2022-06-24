package edu.cornell.cs.apl.viaduct.security.solver2

import edu.cornell.cs.apl.viaduct.security.SecurityLattice
import edu.cornell.cs.apl.viaduct.algebra.solver2.Constraint as ComponentConstraint

/** A [SecurityLattice] constraint. */
typealias Constraint<C, V, T> = ComponentConstraint<C, ComponentVariable<V>, T>
