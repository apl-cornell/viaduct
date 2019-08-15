package edu.cornell.cs.apl.viaduct.util;

/**
 * A Brouwerian lattice is a bounded lattice that supports a "relative pseudocomplement"
 * operation -> where A -> B defines a maximal element x such that A & x <= B.
 * This is also called an implicated lattice:
 * see https://en.wikipedia.org/wiki/Pseudocomplement#Relative_pseudocomplement
 */
public interface BrouwerianLattice<T extends BrouwerianLattice<T>> extends Lattice<T> {

  /**
   * {@code t1.relativePseudocomplement(t2)} is the greatest solution to
   * {@code t2.meet(x).lessThanOrEqualTo(t1)}.
   */
  T relativePseudocomplement(T that);
}
