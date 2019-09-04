package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.Name;
import edu.cornell.cs.apl.viaduct.imp.parser.Located;

/** A name that is referenced before it is ever defined. */
public final class UndefinedNameError extends CompilationError {
  private final Name name;
  private final Located location;

  public <N extends Located & Name> UndefinedNameError(N name) {
    this.name = name;
    this.location = name;
  }

  @Override
  protected String getCategory() {
    return "Naming Error";
  }

  @Override
  protected String getSource() {
    return location.getSourceLocation().getSourcePath();
  }
}
