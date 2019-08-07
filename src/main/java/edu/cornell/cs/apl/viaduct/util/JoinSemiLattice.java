package edu.cornell.cs.apl.viaduct.util;

/** A set with least upper bounds and a least element. */
public interface JoinSemiLattice<T extends JoinSemiLattice<T>> extends PartialOrder<T> {
  /** Least upper bound of {@code this} and {@code that}. */
  T join(T that);
}
