package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;

/** turn unguarded loops into while and for loops. */
public class UnelaborationVisitor extends FormatBlockVisitor {
  @Override
  public StmtNode visit(LoopNode loop) {
    StmtNode body = loop.getBody().accept(this);
    StmtNode firstLoopStmt = null;

    if (body instanceof BlockNode) {
      firstLoopStmt = ((BlockNode)body).getFirstStmt();

    } else {
      firstLoopStmt = body;
    }

    if (firstLoopStmt != null && firstLoopStmt instanceof IfNode) {
      IfNode ifNode = (IfNode)firstLoopStmt;
      StmtNode elseNode = (StmtNode)ifNode.getElseBranch();
      StmtNode lastElse = null;
      if (elseNode instanceof BlockNode) {
        lastElse = ((BlockNode)elseNode).getLastStmt();

      } else {
        lastElse = elseNode;
      }

      if (lastElse != null && lastElse instanceof BreakNode) {
        BreakNode elseBreak = (BreakNode)lastElse;
        ExpressionNode elseBreakLevel = elseBreak.getLevel();
        if (elseBreakLevel instanceof LiteralNode) {
          ImpValue levelVal = ((LiteralNode)elseBreakLevel).getValue();

          // decompile loop into a while loop
          if (levelVal instanceof IntegerValue
              && ((IntegerValue)levelVal).getValue() == 0) {

            ExpressionNode whileGuard = ifNode.getGuard();
            StmtNode whileBody = ifNode.getThenBranch();
            return new WhileNode(whileGuard, whileBody);
          }
        }
      }
    }

    return super.visit(loop);
  }
}
