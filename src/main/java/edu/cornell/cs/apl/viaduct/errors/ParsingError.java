package edu.cornell.cs.apl.viaduct.errors;

import com.google.common.collect.ImmutableList;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import java.io.PrintStream;

/** Thrown by the parser then it runs into an unexpected token. */
public class ParsingError extends CompilationError {
  private final SourceRange location;
  private final String actualToken;
  private final ImmutableList<String> expectedTokens;

  /**
   * Constructor.
   *
   * @param location location of the unexpected token
   * @param actualToken token that was encountered
   * @param expectedTokens tokens that would have been valid
   */
  public ParsingError(SourceRange location, String actualToken, Iterable<String> expectedTokens) {
    this.location = location;
    this.actualToken = actualToken;
    this.expectedTokens = ImmutableList.copyOf(expectedTokens);
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

    output.println("I ran into an issue while parsing this file.");

    output.println();
    location.showInSource(output);

    output.println("I was expecting one of these:");
    output.println();
    output.println(String.join(", ", expectedTokens));

    output.println();
    output.println("Instead, I found:");
    output.println();
    output.println(actualToken);

    output.println();
  }
}
