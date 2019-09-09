package edu.cornell.cs.apl.viaduct.imp.ast;

import java.io.PrintStream;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/** Objects that name things. */
public interface Name {
  /** The given name. */
  String getName();

  /** Class of things this objects names. */
  String getNameCategory();

  /** Print the name of this object to the given stream with colors. */
  default void print(PrintStream output) {
    output.print(Ansi.ansi().fg(Color.BLUE).a(getName()).reset());
  }
}
