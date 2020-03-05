package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import edu.cornell.cs.apl.viaduct.passes.ElaborationKt;
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode;
import java.io.IOException;
import java.io.PrintStream;

@Command(
    name = "specification",
    description = "Generate UC ideal functionality from source program")
public class SpecificationCommand extends BaseCommand {

  @Override
  public void run() throws IOException {
    final ProgramNode program = ElaborationKt.elaborated(this.input.parse());

    // TODO: generate specification!!

    try (PrintStream writer = output.newOutputStream()) {
      program.getAsDocument().print(writer, 80, true);
    }
  }
}
