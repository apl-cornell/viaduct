package edu.cornell.cs.apl.viaduct.imp.informationflow;

import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;
import java.util.Map;

final class CheckProgramVisitor extends AbstractProgramVisitor<CheckProgramVisitor, Void, Void> {
  private final CheckDeclarationVisitor declarationVisitor = new CheckDeclarationVisitor();
  private final Map<HostName, HostDeclarationNode> hosts;

  CheckProgramVisitor() {
    this.hosts = Map.of();
  }

  private CheckProgramVisitor(ProgramNode program) {
    this.hosts = program.hosts();
  }

  @Override
  protected TopLevelDeclarationVisitor<Void> getDeclarationVisitor() {
    return declarationVisitor;
  }

  @Override
  protected CheckProgramVisitor enter(ProgramNode node) {
    return new CheckProgramVisitor(node);
  }

  @Override
  protected Void leave(ProgramNode node, CheckProgramVisitor visitor, Iterable<Void> declarations) {
    return null;
  }

  private final class CheckDeclarationVisitor implements TopLevelDeclarationVisitor<Void> {
    @Override
    public Void visit(HostDeclarationNode node) {
      return null;
    }

    @Override
    public Void visit(ProcessDeclarationNode node) {
      CheckStmtVisitor.run(node.getBody(), hosts);
      return null;
    }
  }
}
