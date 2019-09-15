package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;

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

  private static final class CheckDeclarationVisitor implements TopLevelDeclarationVisitor<Void> {
    @Override
    public Void visit(HostDeclarationNode node) {
      return null;
    }

    @Override
    public Void visit(ProcessDeclarationNode node) {
      CheckStmtVisitor.run(node.getBody());
      return null;
    }
  }
}
