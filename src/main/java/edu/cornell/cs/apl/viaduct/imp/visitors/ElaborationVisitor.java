package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;

/** Elaborate derived AST nodes into internal ones.. */
public class ElaborationVisitor extends FormatBlockVisitor {
  /** rewrite while loop into loop-and-break. */
  @Override
  public StmtNode visit(WhileNode whileNode) {
    ExpressionNode newGuard = whileNode.getGuard().accept(this);
    StmtNode newBody = whileNode.getBody().accept(this);
    BreakNode loopBreak = BreakNode.create(LiteralNode.create(IntegerValue.create(0)));
    IfNode ifNode = IfNode.create(newGuard, newBody, BlockNode.create(loopBreak));
    LoopNode loop = LoopNode.create(BlockNode.create(ifNode));
    return loop;
  }

  /** Rewrite for loops into while loops. */
  @Override
  public StmtNode visit(ForNode forNode) {
    BlockNode whileBody = BlockNode.create(forNode.getBody(), forNode.getUpdate());
    WhileNode whileLoop = WhileNode.create(forNode.getGuard(), whileBody);

    StmtNode newInit = forNode.getInitialize().accept(this);
    StmtNode elaboratedWhile = whileLoop.accept(this);
    return BlockNode.create(newInit, elaboratedWhile).accept(this);
  }
}
