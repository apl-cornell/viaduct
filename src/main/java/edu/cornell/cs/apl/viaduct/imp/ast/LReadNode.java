package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.visitors.LExprVisitor;

/** Read the value of a variable. */
public final class LReadNode extends AbstractReadNode implements LExpressionNode {
  public LReadNode(Variable variable) {
    super(variable);
  }

  public LReadNode(Binding<ImpAstNode> binding) {
    super(binding);
  }

  public LReadNode(String name) {
    super(name);
  }

  @Override
  public <R> R accept(LExprVisitor<R> v) {
    return v.visit(this);
  }
}
