package edu.cornell.cs.apl.viaduct.imp.typing;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import io.vavr.Tuple2;

/**
 * Check that value types are cohesive (e.g. only integers are added together). Label checking is a
 * separate step.
 */
final class CheckProgramVisitor extends AbstractProgramVisitor<CheckProgramVisitor, Void, Void> {
  private final CheckStmtVisitor statementVisitor;

  CheckProgramVisitor() {
    this.statementVisitor = new CheckStmtVisitor();
  }

  @Override
  public StmtVisitor<Void> getStatementVisitor() {
    return statementVisitor;
  }

  @Override
  protected CheckProgramVisitor enter(ProcessName process, StatementNode body) {
    return new CheckProgramVisitor();
  }

  @Override
  protected Void leave(ProgramNode node, Iterable<Tuple2<ProcessName, Void>> processes) {
    return null;
  }
}
