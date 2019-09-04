package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AnfVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ElaborationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
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
  public Void call() throws Exception {
    // parse
    ProgramNode program = this.input.parse();

    // print (de-parse)
    try (PrintStream writer = this.output.newOutputStream()) {
      if (this.enableElaboration) {
        program = new ElaborationVisitor().run(program);
      }

      if (this.enableAnf) {
        program = new AnfVisitor().run(program);
      }

      writer.println(PrintVisitor.run(program));
    }
    return null;
  }
}
