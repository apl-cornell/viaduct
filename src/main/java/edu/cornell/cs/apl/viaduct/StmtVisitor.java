package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.surface.AssignNode;
import edu.cornell.cs.apl.viaduct.surface.BlockNode;
import edu.cornell.cs.apl.viaduct.surface.IfNode;
import edu.cornell.cs.apl.viaduct.surface.SkipNode;
import edu.cornell.cs.apl.viaduct.surface.VarDeclNode;

/** interface for visiting statements. */
public interface StmtVisitor<R> {
  R visit(SkipNode skipNode);

  R visit(VarDeclNode varDeclNode);

  R visit(AssignNode assignNode);

  R visit(BlockNode blockNode);

  R visit(IfNode ifNode);
}
