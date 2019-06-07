package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;

import java.util.ArrayList;
import java.util.List;

/** removes empty blocks. */
public class EmptyBlockVisitor extends IdentityVisitor {
  public StmtNode run(StmtNode program) {
    return program.accept(this);
  }

  @Override
  public StmtNode visit(BlockNode block) {
    List<StmtNode> stmts = new ArrayList<>();
    for (StmtNode stmt : block) {
      StmtNode newStmt = stmt.accept(this);

      if (!(newStmt instanceof BlockNode && ((BlockNode) newStmt).size() == 0)) {
        stmts.add(newStmt);
      }
    }

    return new BlockNode(stmts);
  }
}
