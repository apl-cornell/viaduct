package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** While loops. */
@AutoValue
public abstract class WhileNode extends StmtNode {
  /**
   * If {@code guard} evaluates to true, execute {@code body}, then loop; otherwise skip.
   *
   * @param guard loop guard
   * @param body loop body
   */
  public static WhileNode create(ExpressionNode guard, StmtNode body) {
    return new AutoValue_WhileNode(guard, body);
  }

  public abstract ExpressionNode getGuard();

  public abstract StmtNode getBody();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
