package edu.cornell.cs.apl.viaduct.errors;

import edu.cornell.cs.apl.viaduct.imp.ast.Name;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceRange;
import java.io.PrintStream;

/** A name that is referenced before it is ever defined. */
public class UndefinedNameError extends CompilationError {
  private final Name name;
  private final SourceRange location;

  public <N extends HasLocation & Name> UndefinedNameError(N name) {
    this.name = name;
    this.location = name.getSourceLocation();
  }

  @Override
  protected String getCategory() {
    return "Naming Error";
  }

  @Override
  protected String getSource() {
    return location.getSourcePath();
  }

  @Override
  public void print(PrintStream output) {
    super.print(output);

    output.print("I cannot find a " + name.getNameCategory() + " named ");
    Printer.run(name, output);
    output.println(':');

    output.println();
    location.showInSource(output);

    // TODO: show similar names in context
  }
}
