package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** used in RHS of expressions (ie.interpreted as an actual array access). */
public final class ArrayAccessNode extends AbstractArrayAccessNode
    implements ExpressionNode {

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  public ArrayAccessNode(Variable v, ExpressionNode ind) {
    super(v, ind);
  }
}

