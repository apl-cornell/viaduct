package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.errors.TodoException;
import edu.cornell.cs.apl.viaduct.imp.parsing.Located;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;

/** A location that can run (one or more) processes. */
@AutoValue
public abstract class HostName implements Name, Located, Comparable<HostName> {
  public static HostName create(String name) {
    return create(name, false);
  }

  public static HostName create(String name, boolean isSynthetic) {
    return new AutoValue_HostName(name, isSynthetic);
  }

  // TODO: this will interact badly with .equals(). For example, compareTo is broken as is.
  public abstract boolean isSynthetic();

  @Override
  public final String getNameCategory() {
    return "host";
  }

  @Override
  public final SourceRange getSourceLocation() {
    throw new TodoException();
  }

  @Override
  public final int compareTo(HostName that) {
    return this.getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }
}
