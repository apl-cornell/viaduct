package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Send the value of an expression to a process. */
@AutoValue
public abstract class SendNode
    extends StatementNode implements CommunicationNode
{
  /** default is *not* external communication, unless explicitly set. */
  public static Builder builder() {
    return
        new AutoValue_SendNode.Builder()
        .setExternalCommunication(false);
  }

  public abstract Builder toBuilder();

  /** Process to send the value to. */
  public abstract ProcessName getRecipient();

  /** Value to send. */
  public abstract ExpressionNode getSentExpression();

  /** flag for external communication with a host. */
  public abstract boolean isExternalCommunication();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setRecipient(ProcessName recipient);

    public abstract Builder setSentExpression(ExpressionNode sentExpression);

    public abstract Builder setExternalCommunication(boolean external);

    public abstract SendNode build();
  }
}
