package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;

public interface MambaStatementVisitor<R> {
  R visit(MambaRegIntDeclarationNode node);

  R visit(MambaBlockNode node);
}
