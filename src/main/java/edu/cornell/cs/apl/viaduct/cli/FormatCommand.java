package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AnfVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ElaborationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import java.io.BufferedWriter;

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

    // typecheck
    // final TypeCheckVisitor typeChecker = new TypeCheckVisitor();
    // typeChecker.run(program);

    // print (de-parse)
    try (BufferedWriter writer = this.output.newOutputWriter()) {
      if (this.enableElaboration) {
        program = new ElaborationVisitor().run(program);
      }

      if (this.enableAnf) {
        program = new AnfVisitor().run(program);
      }

      writer.write(PrintVisitor.run(program));
      writer.newLine();
    }
    return null;
  }
}
