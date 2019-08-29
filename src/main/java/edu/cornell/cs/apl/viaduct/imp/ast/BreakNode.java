package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

@AutoValue
public abstract class BreakNode extends StatementNode {
  public static BreakNode create(ExpressionNode level) {
    return new AutoValue_BreakNode(level);
  }

  public abstract ExpressionNode getLevel();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
