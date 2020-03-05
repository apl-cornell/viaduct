package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.passes.CheckingKt;
import edu.cornell.cs.apl.viaduct.passes.ElaborationKt;
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode;
import java.io.IOException;
import java.io.PrintStream;

@Command(name = "format", description = "Pretty print source program")
public class FormatCommand extends BaseCommand {
  @Option(
      name = {"-e", "--elaborated"},
      description = "Show internal representation.")
  private boolean enableElaboration;

  @Option(
      name = {"-c", "--check"},
      description = "Type check the program before printing.")
  private boolean enableChecks;

  @Override
  public void run() throws IOException {
    ProgramNode program = this.input.parse();

    if (this.enableChecks) {
      CheckingKt.check(ElaborationKt.elaborated(program));
    }

    try (PrintStream writer = this.output.newOutputStream()) {
      if (this.enableElaboration) {
        program = ElaborationKt.elaborated(program).toSurfaceNode();
      }
      program.getAsDocument().print(writer, 80, true);
    }
  }
}
