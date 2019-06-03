package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import java.util.Map;

/** Interpreter for Imp programs. */
public class Interpreter {
  public Interpreter() {}

  /** Return the value the given expression evaluates to. */
  public ImpValue run(ExpressionNode expression) {
    return new InterpretProcessVisitor().run(expression);
  }

  /** Execute the given statement and return the resulting store. */
  public Store run(StmtNode statement) {
    return new InterpretProcessVisitor().run(statement);
  }

  /** Execute the code on all processes in the configuration, and return their local stores. */
  public Map<Host, Store> run(ProcessConfigurationNode configuration) {
    return new InterpretProcessConfiguration(configuration).run();
  }
}
