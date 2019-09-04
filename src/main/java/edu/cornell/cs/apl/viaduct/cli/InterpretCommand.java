package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Store;
import edu.cornell.cs.apl.viaduct.imp.typing.TypeChecker;
import java.io.PrintStream;
import java.util.Map;

@Command(name = "interpret", description = "Execute program and print its final state")
public class InterpretCommand extends BaseCommand {
  @Override
  public Void call() throws Exception {
    // parse
    final ProgramNode program = this.input.parse();

    // typecheck
    TypeChecker.run(program);

    // interpret
    final Map<ProcessName, Store> stores = Interpreter.run(program);

    try (PrintStream writer = this.output.newOutputStream()) {
      boolean first = true;
      for (Map.Entry<ProcessName, Store> entry : stores.entrySet()) {
        if (!first) {
          writer.println();
        }

        writer.println("process " + entry.getKey() + ":");
        entry.getValue().print(writer);

        first = false;
      }
    }
    return null;
  }
}
