package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.Name;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;

/** A (variable, host, process etc.) name declared multiple times. */
public class NameClashError extends CompilationError {
  private final Name name;
  private final HasLocation firstDeclaration;
  private final HasLocation secondDeclaration;

  /**
   * Create an error description given two definition sites of the same name.
   *
   * @param firstDeclaration original location the name is declared
   * @param secondDeclaration current declaration
   */
  public <N extends HasLocation & Name> NameClashError(
      HasLocation firstDeclaration, N secondDeclaration) {
    this.name = secondDeclaration;
    this.firstDeclaration = firstDeclaration;
    this.secondDeclaration = secondDeclaration;

    assert firstDeclaration
        .getSourceLocation()
        .getSourcePath()
        .equals(secondDeclaration.getSourceLocation().getSourcePath());
  }

  @Override
  protected String getCategory() {
    return "Name Clash";
  }

  @Override
  protected String getSource() {
    return firstDeclaration.getSourceLocation().getSourcePath();
  }
}
