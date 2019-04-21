package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.AstNode;

/** Generic interface for expression and statement nodes. */
public class ImpAstNode implements AstNode {
  public String getTitle() {
    return this.toString();
  }
}
