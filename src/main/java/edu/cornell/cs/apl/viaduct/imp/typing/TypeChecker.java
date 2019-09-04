package edu.cornell.cs.apl.viaduct.imp.typing;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;

public final class TypeChecker {
  /** Assert that the given statement is type correct. */
  public static void run(StatementNode statement) {
    statement.accept(new CheckStmtVisitor());
  }

  /** Assert that the given program is type correct. */
  public static void run(ProgramNode program) {
    program.accept(new CheckProgramVisitor());
  }
}
