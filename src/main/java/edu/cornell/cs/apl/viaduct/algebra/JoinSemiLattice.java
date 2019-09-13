package edu.cornell.cs.apl.viaduct.algebra;

/** A set that supports binary least upper bounds. */
public interface JoinSemiLattice<T extends JoinSemiLattice<T>> extends PartialOrder<T> {
  /** Least upper bound of {@code this} and {@code that}. */
  T join(T that);
}
