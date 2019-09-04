package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.errors.TodoException;
import edu.cornell.cs.apl.viaduct.imp.parser.Located;
import edu.cornell.cs.apl.viaduct.imp.parser.SourceRange;

/** Process names. Processes execute code, and can send and receive messages. */
@AutoValue
public abstract class ProcessName implements Name, Located, Comparable<ProcessName> {
  private static final ProcessName MAIN = ProcessName.create("main");

  public static ProcessName create(String name) {
    return new AutoValue_ProcessName(name);
  }

  /** Get the default process name that corresponds to a host. */
  public static ProcessName create(Host host) {
    return create(host.getName());
  }

  /** Name of the entry process. */
  public static ProcessName getMain() {
    return MAIN;
  }

  @Override
  public final String getNameCategory() {
    return "process";
  }

  @Override
  public final SourceRange getSourceLocation() {
    throw new TodoException();
  }

  @Override
  public final int compareTo(ProcessName that) {
    return getName().compareTo(that.getName());
  }

  @Override
  public final String toString() {
    return getName();
  }
}
