package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Update the value associated with a reference. */
@AutoValue
public abstract class AssignNode extends StatementNode {
  public static AssignNode create(ReferenceNode lhs, ExpressionNode rhs) {
    return new AutoValue_AssignNode(lhs, rhs);
  }

  public abstract ReferenceNode getLhs();

  public abstract ExpressionNode getRhs();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }
}
