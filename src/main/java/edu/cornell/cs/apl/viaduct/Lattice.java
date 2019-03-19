package edu.cornell.cs.apl.viaduct;

public interface Lattice<T extends Lattice<T>> {
  T join(T y);
}
