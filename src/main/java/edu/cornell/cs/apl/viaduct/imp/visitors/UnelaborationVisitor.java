package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;

/** turn unguarded loops into while and for loops. */
public class UnelaborationVisitor extends FormatBlockVisitor {
  @Override
  public StatementNode visit(LoopNode loop) {
    final StatementNode body = loop.getBody().accept(this);
    StatementNode firstLoopStmt;

    if (body instanceof BlockNode && ((BlockNode) body).size() >= 1) {
      firstLoopStmt = ((BlockNode) body).getFirstStmt();
    } else {
      firstLoopStmt = body;
    }

    if (firstLoopStmt instanceof IfNode) {
      IfNode ifNode = (IfNode) firstLoopStmt;
      StatementNode elseNode = ifNode.getElseBranch();
      StatementNode lastElse;

      if (elseNode instanceof BlockNode && ((BlockNode) elseNode).size() >= 1) {
        lastElse = ((BlockNode) elseNode).getLastStmt();
      } else {
        lastElse = elseNode;
      }

      if (lastElse instanceof BreakNode && ((BreakNode) lastElse).getLevel() == 1) {
        ExpressionNode whileGuard = ifNode.getGuard();
        StatementNode whileBody = ifNode.getThenBranch();
        return WhileNode.create(whileGuard, whileBody);
      }
    }

    return super.visit(loop);
  }
}
