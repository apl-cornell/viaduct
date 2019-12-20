package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import javax.annotation.Nullable;

/** Receive a value from a process. */
@AutoValue
public abstract class ReceiveNode extends StatementNode implements CommunicationNode {
  /** default is *not* external communication, unless explicitly set. */
  public static Builder builder() {
    return new AutoValue_ReceiveNode.Builder().setExternalCommunication(false);
  }

  public abstract Builder toBuilder();

  /** Location to store the received value in. */
  // TODO: turn into a reference node
  public abstract Variable getVariable();

  /** Expected type of the value to be received. */
  // TODO: remove this. Type information should be stored somewhere else.
  public abstract @Nullable ImpBaseType getReceiveType();

  /** Process to receive a value from. */
  public abstract ProcessName getSender();

  /** flag for external communication with a host. */
  public abstract boolean isExternalCommunication();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @AutoValue.Builder
  public abstract static class Builder extends ImpAstNode.Builder<Builder> {
    public abstract Builder setVariable(Variable variable);

    public abstract Builder setReceiveType(ImpBaseType receivedType);

    public abstract Builder setSender(ProcessName sender);

    public abstract Builder setExternalCommunication(boolean external);

    public abstract ReceiveNode build();
  }
}
