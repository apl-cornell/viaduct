package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import java.io.PrintStream;

/** Thrown when the lexer encounters an illegal character. */
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

    output.println("I ran into a character I did not expect:");

    output.println();
    location.showInSource(output);
  }
}
