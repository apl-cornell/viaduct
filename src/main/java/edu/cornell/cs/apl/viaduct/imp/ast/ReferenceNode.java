package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;

/**
 * The address of a memory location such as a variable or a specific index in an array.
 *
 * <p>References can be written to or read from.
 */
public abstract class ReferenceNode extends ImpAstNode {
  public abstract <R> R accept(ReferenceVisitor<R> v);

  @Override
  public final <R> R accept(ImpAstVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
