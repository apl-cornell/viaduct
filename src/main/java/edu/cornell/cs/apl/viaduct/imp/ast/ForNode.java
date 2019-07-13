package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** For loop. */
@AutoValue
public abstract class ForNode extends StmtNode {
  /**
   * Initialize with {@code init}, loop until {@code guard} is false, and then update loop variable
   * with {@code update}.
   *
   * @param init initializer
   * @param guard loop guard
   * @param update loop variable update
   * @param body loop body
   */
  public static ForNode create(
      StmtNode init, ExpressionNode guard, StmtNode update, StmtNode body) {
    return new AutoValue_ForNode(init, guard, update, body);
  }

  public abstract StmtNode getInitialize();

  public abstract ExpressionNode getGuard();

  public abstract StmtNode getUpdate();

  public abstract StmtNode getBody();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
