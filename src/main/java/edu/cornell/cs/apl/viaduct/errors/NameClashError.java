package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.Name;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import java.io.PrintStream;

/** A (variable, host, process etc.) name declared multiple times. */
public class NameClashError extends CompilationError {
  private final Name name;
  private final SourceRange firstDeclaration;
  private final SourceRange secondDeclaration;

  /**
   * Create an error description given two definition sites of the same name.
   *
   * @param firstDeclaration original location the name is declared
   * @param secondDeclaration current declaration
   */
  public <N extends HasLocation & Name> NameClashError(
      HasLocation firstDeclaration, N secondDeclaration) {
    this.name = secondDeclaration;
    this.firstDeclaration = firstDeclaration.getSourceLocation();
    this.secondDeclaration = secondDeclaration.getSourceLocation();

    assert this.firstDeclaration.getSourcePath().equals(this.secondDeclaration.getSourcePath());
  }

  @Override
  protected String getCategory() {
    return "Name Clash";
  }

  @Override
  protected String getSource() {
    return firstDeclaration.getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.print("This file has multiple ");
    name.print(output);
    output.println(" declarations. One here:");

    output.println();
    firstDeclaration.showInSource(output);

    output.println("And another one here:");

    output.println();
    secondDeclaration.showInSource(output);
  }
}
