package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import java.io.PrintStream;

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

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.println("I encountered a process declaration is a host configuration file:");

    output.println();
    declaration.getName().getSourceLocation().showInSource(output);

    output.println("These files can only contain trust declarations for hosts.");

    output.println();
  }
}
