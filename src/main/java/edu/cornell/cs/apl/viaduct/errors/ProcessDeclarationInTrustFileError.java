package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;

/** Thrown when there is a process declaration in a host trust configuration file. */
public class ProcessDeclarationInTrustFileError extends CompilationError {
  private final ProcessDeclarationNode declaration;

  public ProcessDeclarationInTrustFileError(ProcessDeclarationNode declaration) {
    this.declaration = declaration;
  }

  @Override
  protected String getCategory() {
    return "Unexpected Process Declaration";
  }

  @Override
  protected String getSource() {
    return declaration.getSourceLocation().getSourcePath();
  }
}
