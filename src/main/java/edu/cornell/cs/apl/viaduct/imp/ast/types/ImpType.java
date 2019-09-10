package edu.cornell.cs.apl.viaduct.imp.ast.types;

import java.io.PrintStream;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/** Types for Imp values. */
public interface ImpType {
  /** Print this type to the given stream with colors. */
  default void print(PrintStream output) {
    output.print(Ansi.ansi().fg(Color.YELLOW).a(this).reset());
  }
}
