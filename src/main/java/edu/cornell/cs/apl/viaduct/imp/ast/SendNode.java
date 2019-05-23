package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** send value to a process. */
public class SendNode extends StmtNode {
  private final ExpressionNode sentExpr;
  private final String recipient;

  public SendNode(String r, ExpressionNode expr) {
    this.sentExpr = expr;
    this.recipient = r;
  }

  @Override
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
