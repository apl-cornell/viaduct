package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** send value to a process. */
public class SendNode extends StmtNode {
  private final ExpressionNode sentExpr;
  private final Host recipient;

  public SendNode(Host r, ExpressionNode expr) {
    this.sentExpr = expr;
    this.recipient = r;
  }

  public Host getRecipient() {
    return this.recipient;
  }

  public ExpressionNode getSentExpr() {
    return this.sentExpr;
  }

  @Override
  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof SendNode) {
      SendNode otherSend = (SendNode) other;
      return otherSend.sentExpr.equals(this.sentExpr) && otherSend.recipient.equals(this.recipient);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.sentExpr, this.recipient);
  }
}
