package edu.cornell.cs.apl.viaduct.imp.typing;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractTopLevelDeclarationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;

/**
 * Check that value types are cohesive (e.g. only integers are added together). Label checking is a
 * separate step.
 */
final class CheckProgramVisitor extends AbstractProgramVisitor<CheckProgramVisitor, Void, Void> {
  private final CheckDeclarationVisitor declarationVisitor = new CheckDeclarationVisitor();

  @Override
  protected TopLevelDeclarationVisitor<Void> getDeclarationVisitor() {
    return declarationVisitor;
  }

  @Override
  protected CheckProgramVisitor enter(ProgramNode node) {
    return this;
  }

  @Override
  protected Void leave(ProgramNode node, CheckProgramVisitor visitor, Iterable<Void> declarations) {
    return null;
  }

  private static final class CheckDeclarationVisitor
      extends AbstractTopLevelDeclarationVisitor<CheckDeclarationVisitor, Void, Void> {
    private final CheckStmtVisitor statementVisitor = new CheckStmtVisitor();

    @Override
    protected StmtVisitor<Void> getStatementVisitor() {
      return statementVisitor;
    }

    @Override
    protected CheckDeclarationVisitor enter(TopLevelDeclarationNode node) {
      return this;
    }

    @Override
    protected Void leave(TopLevelDeclarationNode node, CheckDeclarationVisitor visitor) {
      return null;
    }
  }
}
