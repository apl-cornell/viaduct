package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * A wrapper for metadata information. The main use of this class is getting AutoValue to ignore
 * certain fields in the definitions of {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * <p>All instances of this class compare equal to each other and to {@code null} no matter the
 * wrapped data. Additionally, all instances hash to the same value.
 *
 * @param <T> type of wrapped values
 */
final class Metadata<T> {
  private final T data;

  Metadata(@Nonnull T data) {
    this.data = Objects.requireNonNull(data);
  }

  T getData() {
    return data;
  }

  @Override
  public boolean equals(Object that) {
    return that == null || (that instanceof Metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(null);
  }

  @Override
  public String toString() {
    return Objects.toString(getData());
  }
}
