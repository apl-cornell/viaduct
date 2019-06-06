package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Send the value of an expression to a host. */
public final class SendNode extends StmtNode {
  private final Host recipient;
  private final ExpressionNode sentExpression;

  public SendNode(Host host, ExpressionNode sentExpression) {
    this.sentExpression = Objects.requireNonNull(sentExpression);
    this.recipient = Objects.requireNonNull(host);
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

    if (!(o instanceof SendNode)) {
      return false;
    }

    final SendNode that = (SendNode) o;
    return Objects.equals(this.recipient, that.recipient)
        && Objects.equals(this.sentExpression, that.sentExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.recipient, this.sentExpression);
  }

  @Override
  public String toString() {
    return String.format("(send %s to %s)", this.sentExpression, this.recipient);
  }
}
