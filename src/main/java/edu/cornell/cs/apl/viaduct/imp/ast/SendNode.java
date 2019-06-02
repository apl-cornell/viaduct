package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** send value to a process. */
public class SendNode extends StmtNode {
  private final Host recipient;
  private final ExpressionNode sentExpression;

  public SendNode(Host host, ExpressionNode expression) {
    this.sentExpression = expression;
    this.recipient = host;
  }

  public Host getRecipient() {
    return this.recipient;
  }

  public ExpressionNode getSentExpression() {
    return this.sentExpression;
  }

  @Override
  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final SendNode that = (SendNode) o;
    return this.recipient.equals(that.recipient) && this.sentExpression.equals(that.sentExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.recipient, this.sentExpression);
  }
}
