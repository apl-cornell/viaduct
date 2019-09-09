package edu.cornell.cs.apl.viaduct.imp.ast.values;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import java.io.PrintStream;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/** The result of evaluating an {@link ExpressionNode}. */
public interface ImpValue {
  ImpBaseType getType();

  /** Print this value to the given stream with colors. */
  default void print(PrintStream output) {
    output.print(Ansi.ansi().fg(Color.CYAN).a(this).reset());
  }
}
