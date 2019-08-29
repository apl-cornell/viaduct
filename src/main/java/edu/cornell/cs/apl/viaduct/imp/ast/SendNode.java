package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Send the value of an expression to a host. */
@AutoValue
public abstract class SendNode extends StatementNode {
  /**
   * Send the value of {@code sentExpression} to {@code recipient}.
   *
   * @param recipient process to send the value to
   * @param sentExpression the value to send
   */
  public static SendNode create(ProcessName recipient, ExpressionNode sentExpression) {
    return new AutoValue_SendNode(recipient, sentExpression);
  }

  public abstract ProcessName getRecipient();

  public abstract ExpressionNode getSentExpression();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
