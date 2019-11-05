package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaArrayLoadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaMuxNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaNegationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;

public interface MambaExpressionVisitor<R> {
  R visit(MambaIntLiteralNode node);

  R visit(MambaReadNode node);

  R visit(MambaArrayLoadNode node);

  R visit(MambaBinaryExpressionNode node);

  R visit(MambaNegationNode node);

  R visit(MambaRevealNode node);

  R visit(MambaMuxNode node);
}

