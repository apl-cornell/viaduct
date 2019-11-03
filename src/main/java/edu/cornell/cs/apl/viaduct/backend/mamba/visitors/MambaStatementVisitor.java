package edu.cornell.cs.apl.viaduct.backend.mamba.visitors;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaInputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaOutputNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;

public interface MambaStatementVisitor<R> {
  R visit(MambaRegIntDeclarationNode node);

  R visit(MambaAssignNode node);

  R visit(MambaInputNode node);

  R visit(MambaOutputNode node);

  R visit(MambaIfNode node);

  R visit(MambaWhileNode node);

  R visit(MambaBlockNode node);
}
