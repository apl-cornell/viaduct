package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import java.util.ArrayList;
import java.util.List;

/** removes empty blocks and unnests blocks. */
public class FormatBlockVisitor extends IdentityVisitor {
  @Override
  public StatementNode visit(BlockNode block) {
    List<StatementNode> stmts = new ArrayList<>();
    for (StatementNode stmt : block) {
      StatementNode newStmt = stmt.accept(this);

      if (newStmt instanceof BlockNode) {
        BlockNode newBlock = (BlockNode) newStmt;

        // unnest block
        if (newBlock.size() > 0) {
          for (StatementNode newBlockStmt : newBlock) {
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
