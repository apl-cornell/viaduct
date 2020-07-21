package edu.cornell.cs.apl.viaduct.algebra.solver;

import edu.cornell.cs.apl.viaduct.algebra.HeytingAlgebra;
import java.util.Map;

/** Terms that appear in constraints. */
public interface ConstraintTerm<A extends HeytingAlgebra<A>> {
  /** Returns the value of this term given an assignment of values to every variable in the term. */
  A getValue(Map<VariableTerm<A>, A> solution);

  /** Return the node that will represent this term in the constraint graph. */
  AtomicTerm<A> getNode();
}
