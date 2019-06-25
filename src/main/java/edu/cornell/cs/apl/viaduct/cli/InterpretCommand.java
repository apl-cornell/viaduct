package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Store;
import edu.cornell.cs.apl.viaduct.imp.visitors.TypeCheckVisitor;

import java.io.BufferedWriter;
import java.util.Map;

@Command(name = "interpret", description = "Execute program and print its final state")
public class InterpretCommand extends BaseCommand {
  @Override
  public Void call() throws Exception {
    // parse
    final ProgramNode program = this.input.parse();

    // typecheck
    final TypeCheckVisitor typeChecker = new TypeCheckVisitor();
    typeChecker.run(program);

    // interpret
    final Map<ProcessName, Store> stores = Interpreter.run(program);

    try (BufferedWriter writer = this.output.newOutputWriter()) {
      boolean first = true;
      for (Map.Entry<ProcessName, Store> entry : stores.entrySet()) {
        if (!first) {
          writer.newLine();
        }

        final ProcessName process = entry.getKey();
        final Store store = entry.getValue();

        writer.write("process " + entry.getKey() + ":");
        writer.newLine();
        if (!store.isEmpty()) {
          writer.write(entry.getValue().toString());
        } else {
          writer.write("<empty>");
        }
        writer.newLine();

        first = false;
      }
    }
    return null;
  }
}
