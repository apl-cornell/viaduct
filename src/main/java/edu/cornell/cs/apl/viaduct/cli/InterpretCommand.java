package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import edu.cornell.cs.apl.viaduct.passes.CheckingKt;
import edu.cornell.cs.apl.viaduct.passes.ElaborationKt;
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode;
import java.io.IOException;
import java.io.PrintStream;

@Command(name = "interpret", description = "Execute program and print its final state")
public class InterpretCommand extends BaseCommand {
  @Override
  public void run() throws IOException {
    // Parse
    final ProgramNode program = ElaborationKt.elaborated(this.input.parse());

    // Check
    CheckingKt.check(program);

    // Interpret
    // TODO: interpret and print result;

    try (PrintStream writer = this.output.newOutputStream()) {
      writer.println("TODO");
    }
  }
}
