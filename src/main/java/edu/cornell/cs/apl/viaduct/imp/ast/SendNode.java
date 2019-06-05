package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Send the value of an expression to a host. */
public final class SendNode extends StmtNode {
  private final ProcessName recipient;
  private final ExpressionNode sentExpression;

  /**
   * Send the value of {@code sentExpression} to {@code recipient}.
   *
   * @param recipient process to send the value to
   * @param sentExpression the value to send
   */
  public SendNode(ProcessName recipient, ExpressionNode sentExpression) {
    this.sentExpression = Objects.requireNonNull(sentExpression);
    this.recipient = Objects.requireNonNull(recipient);
  }

  public ProcessName getRecipient() {
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
}
