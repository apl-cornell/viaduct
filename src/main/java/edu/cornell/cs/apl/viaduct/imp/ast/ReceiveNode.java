package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import javax.annotation.Nullable;

/** Receive a value from a host. */
@AutoValue
public abstract class ReceiveNode extends StatementNode {
  /**
   * Receive a value from {@code sender} and store it in {@code variable}.
   *
   * @param variable variable to store the received value in
   * @param sender process to receive the value from
   */
  public static ReceiveNode create(Variable variable, ProcessName sender) {
    return create(variable, null, sender);
  }

  /** constructor. */
  public static ReceiveNode create(
      Variable variable, @Nullable ImpType recvType, ProcessName sender) {
    return new AutoValue_ReceiveNode(variable, recvType, sender);
  }

  public abstract Variable getVariable();

  // TODO: remove this. Type information should be stored somewhere else.
  public abstract @Nullable ImpType getRecvType();

  public abstract ProcessName getSender();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
