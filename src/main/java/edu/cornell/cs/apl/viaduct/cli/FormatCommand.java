package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.parsing.ParserKt;
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode;
import java.io.IOException;
import java.io.PrintStream;

@Command(name = "format", description = "Pretty print source program")
public class FormatCommand extends BaseCommand {
  @Option(
      name = {"-e", "--elaborate"},
      description = "Elaborate derived forms.")
  private boolean enableElaboration;

  @Option(
      name = {"-a", "--anf"},
      description = "Perform A-normal form translation.")
  private boolean enableAnf;

  @Override
  public void run() throws IOException {
    // parse
    ProgramNode program = ParserKt.parse(this.input.newSourceFileKotlin());

    // print
    try (PrintStream writer = this.output.newOutputStream("")) {
      //      if (this.enableElaboration || this.enableAnf) {
      //        program = Elaborator.run(program);
      //      }
      //
      //      if (this.enableAnf) {
      //        program = AnfConverter.run(program);
      //      }

      program.getAsDocument().print(writer, 80, true);
    }
  }
}
