package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;

import java.util.LinkedList;
import java.util.Queue;

/** remove self communication. */
public class SelfCommunicationVisitor extends IdentityVisitor {
  private Host selfHost;
  private Queue<ExpressionNode> sentExprs;

  /** run visitor. */
  public StmtNode run(Host host, StmtNode program) {
    this.selfHost = host;
    this.sentExprs = new LinkedList<>();
    return program.accept(this);
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
}
