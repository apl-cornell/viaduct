package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** send value to a process. */
public class SendNode extends StmtNode {
  String recipient;
  ExpressionNode sentExpr;

  public SendNode(String r, ExpressionNode expr) {
    this.recipient = r;
    this.sentExpr = expr;
  }

  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  public String getRecipient() {
    return this.recipient;
  }

  public ExpressionNode getSentExpr() {
    return this.sentExpr;
  }
}
