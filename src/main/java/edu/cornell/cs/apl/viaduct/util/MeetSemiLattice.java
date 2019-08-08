package edu.cornell.cs.apl.viaduct.util;

/** A set that supports binary greatest lower bounds. */
public interface MeetSemiLattice<T extends MeetSemiLattice<T>> extends PartialOrder<T> {
  /** Greatest lower bound of {@code this} and {@code that}. */
  T meet(T that);
}
