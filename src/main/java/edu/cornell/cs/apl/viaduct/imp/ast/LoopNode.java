package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.Objects;

/** Unguarded loop. */
public class LoopNode implements StmtNode {
  private final StmtNode body;

  public LoopNode(StmtNode body) {
    this.body = body;
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

    if (!(o instanceof LoopNode)) {
      return false;
    }

    LoopNode that = (LoopNode)o;
    return Objects.equals(this.body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.body);
  }

  @Override
  public String toString() {
    return String.format("(loop %s)", this.body);
  }
}
