package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Assert that an expression is true. */
@AutoValue
public abstract class AssertNode extends StatementNode {
  public static AssertNode create(ExpressionNode expression) {
    return new AutoValue_AssertNode(expression);
  }

  public abstract ExpressionNode getExpression();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }
}
