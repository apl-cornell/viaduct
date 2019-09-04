package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.errors.TodoException;
import edu.cornell.cs.apl.viaduct.imp.parser.Located;
import edu.cornell.cs.apl.viaduct.imp.parser.SourceRange;

/** A location that can run (one or more) processes. */
@AutoValue
public abstract class Host implements Name, Located, Comparable<Host> {
  public static Host create(String name) {
    return create(name, false);
  }

  public static Host create(String name, boolean isSynthetic) {
    return new AutoValue_Host(name, isSynthetic);
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
  public final int compareTo(Host that) {
    return this.getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }
}
