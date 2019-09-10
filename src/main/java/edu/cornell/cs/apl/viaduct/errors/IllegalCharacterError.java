package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import java.io.PrintStream;

/** Throws when the lexer encounters an illegal character. */
public class IllegalCharacterError extends CompilationError {
  private final SourceRange location;

  public IllegalCharacterError(SourceRange location) {
    this.location = location;
  }

  @Override
  protected String getCategory() {
    return "Parse Error";
  }

  @Override
  protected String getSource() {
    return location.getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("I cannot interpreter this character:");

    output.println();
    location.showInSource(output);
  }
}
