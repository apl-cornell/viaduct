package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaStatementVisitor;

public interface MambaStatementNode extends MambaAstNode {
  <R> R accept(MambaStatementVisitor<R> v);
}
