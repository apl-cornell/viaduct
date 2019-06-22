package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Receive a value from a host. */
public final class ReceiveNode implements StmtNode {
  private final Variable var;
  private final ProcessName sender;

  /**
   * Receive a value from {@code sender} and store it in {@code variable}.
   *
   * @param variable variable to store the received value in
   * @param sender process to receive the value from
   */
  public ReceiveNode(Variable variable, ProcessName sender) {
    this.var = Objects.requireNonNull(variable);
    this.sender = Objects.requireNonNull(sender);
  }

  public ProcessName getSender() {
    return this.sender;
  }

  public Variable getVariable() {
    return this.var;
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
