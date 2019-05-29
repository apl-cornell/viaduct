package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;
import edu.cornell.cs.apl.viaduct.security.Label;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/** postprocess protocol instantiation.
 *  - remove downgrades
 *  - remove self communication
 *  - remove security labels on variables
 */
public class TargetPostprocessVisitor extends CloneVisitor {
  Host selfHost;
  Queue<ExpressionNode> sentExprs;

  /** set host of current program, then postprocess. */
  public StmtNode postprocess(Host h, StmtNode program) {
    this.selfHost = h;
    this.sentExprs = new LinkedList<>();
    return program.accept(this);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public StmtNode visit(VarDeclNode varDecl) {
    return new VarDeclNode(varDecl.getVariable(), Label.bottom());
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    if (sendNode.getRecipient().equals(this.selfHost)) {
      this.sentExprs.add(sendNode.getSentExpr());
      return new SkipNode();

    } else {
      return super.visit(sendNode);
    }
  }

  @Override
  public StmtNode visit(RecvNode recvNode) {
    if (recvNode.getSender().equals(this.selfHost)) {
      ExpressionNode recvExpr = this.sentExprs.remove();
      return new AssignNode(recvNode.getVar(), recvExpr);

    } else {
      return super.visit(recvNode);
    }
  }

  @Override
  public StmtNode visit(BlockNode block) {
    List<StmtNode> stmts = new ArrayList<>();
    for (StmtNode stmt : block) {
      StmtNode newStmt = stmt.accept(this);

      boolean emptyStmt = false;
      if (newStmt instanceof SkipNode) {
        emptyStmt = true;

      } else if (newStmt instanceof BlockNode) {
        emptyStmt = ((BlockNode)newStmt).size() == 0;
      }

      if (!emptyStmt) {
        stmts.add(newStmt);
      }
    }

    return new BlockNode(stmts);
  }
}
