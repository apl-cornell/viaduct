package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;

public interface MambaExpressionVisitor<R> {
  R visit(MambaIntLiteralNode node);

  R visit(MambaReadNode node);

  R visit(MambaBinaryExpressionNode node);

  R visit(MambaRevealNode node);
}

