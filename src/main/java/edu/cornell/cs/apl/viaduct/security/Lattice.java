package edu.cornell.cs.apl.viaduct.security;

public interface Lattice<T extends Lattice<T>> {
  /** Partial order implied by being a lattice. */
  boolean lessThanOrEqualTo(T other);

  /** Least upper bound of {@code this} and {@code with}. */
  T join(T with);

  /** Greatest lower bound of {@code this} and {@code with}. */
  T meet(T with);
}
