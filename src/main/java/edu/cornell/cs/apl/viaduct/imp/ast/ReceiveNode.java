package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Receive value from a host. */
public final class ReceiveNode extends StmtNode {
  private final Variable var;
  private final Host sender;

  /**
   * Receive the value of this expression during interpretation (if not {@code null}) instead of
   * waiting for a message from {@code sender}.
   *
   * <p>Specified using annotations in the source code. Used for debugging purposes.
   */
  private final ExpressionNode debugReceivedValue;

  public ReceiveNode(Variable variable, Host sender) {
    this(variable, sender, null);
  }

  /**
   * Receive a value from {@code sender} and store it in {@code variable}.
   *
   * @param variable variable to store the received value in
   * @param sender host to receive the value from
   * @param debugReceivedValue the value to use during debugging which sidesteps communication
   */
  public ReceiveNode(Variable variable, Host sender, ExpressionNode debugReceivedValue) {
    this.var = Objects.requireNonNull(variable);
    this.sender = Objects.requireNonNull(sender);
    this.debugReceivedValue = debugReceivedValue;
  }

  public Host getSender() {
    return this.sender;
  }

  public Variable getVariable() {
    return this.var;
  }

  public ExpressionNode getDebugReceivedValue() {
    return this.debugReceivedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ReceiveNode)) {
      return false;
    }

    final ReceiveNode that = (ReceiveNode) o;
    return Objects.equals(this.var, that.var) && Objects.equals(this.sender, that.sender);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.var, this.sender);
  }

  @Override
  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("(receive %s from %s)", this.var, this.sender);
  }
}
