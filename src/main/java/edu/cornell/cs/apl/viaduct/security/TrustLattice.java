package edu.cornell.cs.apl.viaduct.security;

/** A lattice whose elements are interpreted as principals. */
public interface TrustLattice<T extends TrustLattice<T>> {
  /** Decide if {@code this} is trusted to enforce {@code that}'s security policies. */
  boolean actsFor(T that);

  /**
   * The least powerful principal that can act for both {@code this} and {@code that}. This denotes
   * a conjunction of authority.
   */
  T and(T that);

  /**
   * The most powerful principal both {@code this} and {@code that} can act for. This denotes a
   * disjunction of authority.
   */
  T or(T that);
}
