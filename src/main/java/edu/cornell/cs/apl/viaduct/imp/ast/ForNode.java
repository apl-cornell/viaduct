package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** For loop. */
public class ForNode extends StmtNode {
  private final StmtNode initialize;
  private final ExpressionNode guard;
  private final StmtNode update;
  private final StmtNode body;

  /**
   * Initialize with {@code init}, loop until {@code guard} is false, and then update loop variable
   * with {@code update}.
   *
   * @param init initializer
   * @param guard loop guard
   * @param update loop variable update
   * @param body loop body
   */
  public ForNode(StmtNode init, ExpressionNode guard, StmtNode update, StmtNode body) {
    this.initialize = init;
    this.guard = guard;
    this.update = update;
    this.body = body;
  }

  public StmtNode getInitialize() {
    return this.initialize;
  }

  public ExpressionNode getGuard() {
    return this.guard;
  }

  public StmtNode getUpdate() {
    return this.update;
  }

  public StmtNode getBody() {
    return this.body;
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

    if (!(o instanceof ForNode)) {
      return false;
    }

    final ForNode that = (ForNode) o;
    return Objects.equals(this.initialize, that.initialize)
        && Objects.equals(this.guard, that.guard)
        && Objects.equals(this.update, that.update)
        && Objects.equals(this.body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.initialize, this.guard, this.update, this.body);
  }

  @Override
  public String toString() {
    return String.format(
        "(for (%s; %s; %s) do %s)", this.initialize, this.guard, this.update, this.body);
  }
}
