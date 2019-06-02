package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;

public interface Binding<T extends AstNode> {
  String getBinding();
}
