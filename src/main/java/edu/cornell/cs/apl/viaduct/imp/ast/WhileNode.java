package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** If statement. */
public class WhileNode extends StmtNode {
  private final ExpressionNode guard;
  private final StmtNode body;

  /**
   * If {@code guard} evaluates to true, execute {@code body}, then loop; otherwise skip.
   *
   * @param guard loop guard
   * @param body  loop body
   */
  public WhileNode(ExpressionNode guard, StmtNode body) {
    this.guard = guard;
    this.body = body;
  }

  public ExpressionNode getGuard() {
    return this.guard;
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

    if (!(o instanceof WhileNode)) {
      return false;
    }

    final WhileNode that = (WhileNode) o;
    return Objects.equals(this.guard, that.guard)
        && Objects.equals(this.body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.guard, this.body);
  }

  @Override
  public String toString() {
    return String.format("(while %s do %s)", this.guard, this.body);
  }
}
