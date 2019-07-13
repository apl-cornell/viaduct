package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import java.util.ArrayList;
import java.util.List;

/** removes empty blocks and unnests blocks. */
public class FormatBlockVisitor extends IdentityVisitor {
  @Override
  public StmtNode run(StmtNode program) {
    return program.accept(this);
  }

  @Override
  public StmtNode visit(BlockNode block) {
    List<StmtNode> stmts = new ArrayList<>();
    for (StmtNode stmt : block) {
      StmtNode newStmt = stmt.accept(this);

      if (newStmt instanceof BlockNode) {
        BlockNode newBlock = (BlockNode) newStmt;

        // unnest block
        if (newBlock.size() > 0) {
          for (StmtNode newBlockStmt : newBlock) {
            stmts.add(newBlockStmt);
          }
        }

      } else {
        stmts.add(newStmt);
      }
    }

    return BlockNode.create(stmts);
  }
}
