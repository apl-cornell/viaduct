package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAstVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;

public abstract class TopLevelDeclarationNode extends ImpAstNode {
  public abstract <R> R accept(TopLevelDeclarationVisitor<R> v);

  @Override
  public final <R> R accept(ImpAstVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
