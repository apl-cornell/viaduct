package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

@AutoValue
public abstract class BreakNode extends StatementNode {
  /**
   * Create a break node that breaks out of {@code level} number of loops.
   *
   * @param level number of loops to break out of
   * @throws IllegalArgumentException if {@code level} is less than 1
   */
  public static BreakNode create(int level) {
    if (level < 1) {
      throw new IllegalArgumentException("Break level must be at least 1.");
    }
    return new AutoValue_BreakNode(level);
  }

  /** Number of loops to break out of. */
  // TODO: this should REALLY be a label.
  public abstract int getLevel();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
