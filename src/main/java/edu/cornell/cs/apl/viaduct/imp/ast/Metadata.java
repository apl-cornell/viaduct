package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A wrapper for metadata information. The main use of this class is getting AutoValue to ignore
 * certain fields in the definitions of {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * <p>All instances of this class compare equal to each other.
 *
 * @param <T> type of wrapped values
 */
final class Metadata<T> {
  private final T data;

  Metadata(@Nullable T data) {
    this.data = data;
  }

  @Nullable
  T getData() {
    return data;
  }

  @Override
  public boolean equals(Object that) {
    return that instanceof Metadata;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return Objects.toString(getData());
  }
}
