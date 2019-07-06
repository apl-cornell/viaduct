package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;

/** Elaborate derived AST nodes into internal ones.. */
public class ElaborationVisitor extends FormatBlockVisitor {
  /** Rewrite for loops into while loops. */
  @Override
  public StmtNode visit(ForNode forNode) {
    // Elaborate recursive occurrences of for loops
    ForNode newForNode = (ForNode) super.visit(forNode);

    BlockNode whileBody = new BlockNode(newForNode.getBody(), newForNode.getUpdate());
    WhileNode whileLoop = new WhileNode(newForNode.getGuard(), whileBody);
    return new BlockNode(newForNode.getInitialize(), whileLoop);
  }
}
