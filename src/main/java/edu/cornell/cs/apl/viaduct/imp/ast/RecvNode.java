package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** receive value from a process. */
public class RecvNode extends StmtNode {
  private final Variable var;
  private final Host sender;

  public RecvNode(Host s, Variable v) {
    this.var = v;
    this.sender = s;
  }

  public Host getSender() {
    return this.sender;
  }

  public Variable getVar() {
    return this.var;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof RecvNode) {
      RecvNode otherRecv = (RecvNode) other;
      return otherRecv.var.equals(this.var) && otherRecv.sender.equals(this.sender);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.var, this.sender);
  }

  @Override
  public <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
