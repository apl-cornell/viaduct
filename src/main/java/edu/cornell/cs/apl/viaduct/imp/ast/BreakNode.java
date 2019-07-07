package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.Objects;

public final class BreakNode implements StmtNode {
  private final ExpressionNode level;

  public BreakNode(ExpressionNode level) {
    this.level = level;
  }

  public ExpressionNode getLevel() {
    return this.level;
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

    if (!(o instanceof BreakNode)) {
      return false;
    }

    BreakNode that = (BreakNode)o;
    return this.level == that.level;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.level);
  }

  @Override
  public String toString() {
    return String.format("(break %s)", this.level);
  }
}
