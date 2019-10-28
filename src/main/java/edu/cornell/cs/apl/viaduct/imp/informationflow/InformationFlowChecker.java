package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import java.io.Writer;
import java.util.Map;

/**
 * Check that all flows of information within the program are safe.
 *
 * <p>Additionally, adds a minimum-trust label to each node in the AST. This label indicates the
 * minimum amount of trust the host executing that node needs to have for the execution to be
 * secure.
 */
public final class InformationFlowChecker {
  /** Check and decorate a program. */
  public static ProgramNode run(ProgramNode program) {
    program.accept(new CheckProgramVisitor());
    return program;
  }

  /** Write the constraint graph for a statement in the DOT format to {@code output}. */
  public static void exportDotGraph(
      StatementNode statement, Map<HostName, HostDeclarationNode> hosts, Writer output) {
    CheckStmtVisitor.exportDotGraph(statement, hosts, output);
  }
}
