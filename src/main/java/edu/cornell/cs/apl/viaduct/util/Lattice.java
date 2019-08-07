package edu.cornell.cs.apl.viaduct.util;

/** A set with unique least upper and greatest lower bounds. */
public interface Lattice<T extends Lattice<T>> extends JoinSemiLattice<T> {
  /** Greatest lower bound of {@code this} and {@code that}. */
  T meet(T that);
}
