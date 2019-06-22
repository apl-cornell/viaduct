package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.LExprVisitor;

/** array access interpreted as an lvalue (i.e. an array position). */
public final class ArrayIndexNode extends AbstractArrayAccessNode
    implements LExpressionNode {

  public ArrayIndexNode(Variable v, ExpressionNode ind) {
    super(v, ind);
  }

  @Override
  public <R> R accept(LExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("(arrayIndex %s at %s)", this.variable, this.index);
  }
}
