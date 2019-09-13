package edu.cornell.cs.apl.viaduct.algebra.solver;

/** Terms that appear in constraints. */
public interface ConstraintTerm<A> {
  /** Return the node that will represent this term in the constraint graph. */
  AtomicTerm<A> getNode();
}
