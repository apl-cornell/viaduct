package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.parsing.ParsingKt;
import edu.cornell.cs.apl.viaduct.passes.CheckingKt;
import edu.cornell.cs.apl.viaduct.passes.ElaborationKt;
import edu.cornell.cs.apl.viaduct.passes.InformationFlowCheckingKt;
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
    ProgramNode program = ParsingKt.parse(this.input.newSourceFileKotlin());

    try (PrintStream writer = this.output.newOutputStream()) {
      if (this.enableChecks) {
        final edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode elaborated =
            ElaborationKt.elaborated(program);
        CheckingKt.check(elaborated);
        InformationFlowCheckingKt.checkInformationFlow(elaborated);
        // TODO: other checks
      }

      if (this.enableElaboration) {
        program = ElaborationKt.elaborated(program).toSurfaceNode();
      }
      program.getAsDocument().print(writer, 80, true);
    }
  }
}
