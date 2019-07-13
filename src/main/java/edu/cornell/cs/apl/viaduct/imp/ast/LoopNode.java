package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Unguarded loop. */
@AutoValue
public abstract class LoopNode extends StmtNode {
  public static LoopNode create(StmtNode body) {
    return new AutoValue_LoopNode(body);
  }

  public abstract StmtNode getBody();

  @Override
  public final <R> R accept(StmtVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
