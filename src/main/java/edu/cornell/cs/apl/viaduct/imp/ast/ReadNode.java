package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;

/** Read the value of a variable. */
public final class ReadNode extends AbstractReadNode implements ExpressionNode {
  public ReadNode(Variable variable) {
    super(variable);
  }

  public ReadNode(Binding<ImpAstNode> binding) {
    super(binding);
  }

  public ReadNode(String name) {
    super(name);
  }

  @Override
  public <R> R accept(ExprVisitor<R> v) {
    return v.visit(this);
  }
}
