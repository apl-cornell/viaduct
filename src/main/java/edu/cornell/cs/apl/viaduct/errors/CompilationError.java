package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.util.PrintUtil;
import java.io.PrintStream;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiPrintStream;

/**
 * Superclass of all errors caused by bad user input to the compiler.
 *
 * <p>Errors caused by bugs in the compiler do not belong here. They should instead raise a
 * (subclass of) {@link RuntimeException}.
 */
public abstract class CompilationError extends Error {
  /** General description (i.e. title) of the error. */
  protected abstract String getCategory();

  /** Name of the file or description of the source that caused the error. */
  protected abstract String getSource();

  /**
   * Print colored error description to the given stream.
   *
   * <p>The output stream should be ready to handle ANSI color codes. If the output stream does not
   * support color codes, they can be stripped by wrapping the output stream in {@link
   * AnsiPrintStream}.
   */
  public void print(PrintStream output) {
    // Print title line by default
    final String title = "-- " + getCategory().toUpperCase() + " - ";
    final int paddingLength = PrintUtil.LINE_WIDTH - getSource().length() - title.length();
    final String padding = StringUtils.repeat('-', paddingLength);

    output.println(Ansi.ansi().fg(Color.CYAN).a(title).a(padding).a(getSource()).reset());
    output.println();
  }

  @Override
  public final String toString() {
    return PrintUtil.printToString(this::print);
  }
}
