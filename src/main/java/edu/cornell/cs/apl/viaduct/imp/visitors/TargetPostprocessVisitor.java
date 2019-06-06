package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Postprocess protocol instantiation.
 *
 * <p>Remove downgrades, self communication, and security labels on variables.
 */
public class TargetPostprocessVisitor extends IdentityVisitor {
  private Host selfHost;
  private Queue<ExpressionNode> sentExprs;

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
  public StmtNode visit(DeclarationNode declarationNode) {
    return new DeclarationNode(declarationNode.getVariable(), Label.bottom());
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    if (sendNode.getRecipient().equals(new ProcessName(this.selfHost))) {
      this.sentExprs.add(sendNode.getSentExpression());
      return new BlockNode();
    } else {
      return super.visit(sendNode);
    }
  }

  @Override
  public StmtNode visit(ReceiveNode receiveNode) {
    if (receiveNode.getSender().equals(new ProcessName(this.selfHost))) {
      ExpressionNode recvExpr = this.sentExprs.remove();
      return new AssignNode(receiveNode.getVariable(), recvExpr);
    } else {
      return super.visit(receiveNode);
    }
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
