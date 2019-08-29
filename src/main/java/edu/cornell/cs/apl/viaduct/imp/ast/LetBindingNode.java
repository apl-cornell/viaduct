package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** declare and assign temporary variables. */
@AutoValue
public abstract class LetBindingNode extends StatementNode {
  public static LetBindingNode create(Variable var, ExpressionNode rhs) {
    return new AutoValue_LetBindingNode(var, rhs);
  }

  public abstract Variable getVariable();

  public abstract ExpressionNode getRhs();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
