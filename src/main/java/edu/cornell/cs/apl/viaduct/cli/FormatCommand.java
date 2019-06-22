package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import java.io.BufferedWriter;

@Command(name = "format", description = "Pretty print source program")
public class FormatCommand extends BaseCommand {
  @Override
  public Void call() throws Exception {
    final ProgramNode program = this.input.parse();
    try (BufferedWriter writer = this.output.newOutputWriter()) {
      writer.write(new PrintVisitor().run(program));
      writer.newLine();
    }
    return null;
  }
}
