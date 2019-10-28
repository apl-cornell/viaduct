package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaExpressionVisitor;

public interface MambaExpressionNode extends MambaAstNode {
  <R> R accept(MambaExpressionVisitor<R> v);
}
