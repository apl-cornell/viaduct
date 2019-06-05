package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AstVisitor;

/** Generic interface for expression and statement nodes. */
public abstract class ImpAstNode implements AstNode {
  public abstract <R> R accept(AstVisitor<R> v);
}
