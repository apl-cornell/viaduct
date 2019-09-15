package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;

/** Terms that appear in constraints. */
public interface ConstraintTerm<A extends HeytingAlgebra<A>> {
  /** Return the node that will represent this term in the constraint graph. */
  AtomicTerm<A> getNode();
}
