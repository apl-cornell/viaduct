package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/**
 * Explicitly encodes a value that is never provided (as opposed the value implicitly never being
 * provided). A program that tries to reduce an unavailable value gets stuck.
 */
public class UnavailableValue implements ImpValue {
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }

  @Override
  public int hashCode() {
    return Objects.hash();
  }

  @Override
  public String toString() {
    return "unavailable";
  }
}
