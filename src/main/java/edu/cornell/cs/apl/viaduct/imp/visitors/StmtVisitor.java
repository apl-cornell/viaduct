package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;

/** Statement visitor. */
public interface StmtVisitor<R> {
  R visit(SkipNode skipNode);

  R visit(VarDeclNode varDeclNode);

  R visit(AssignNode assignNode);

  R visit(BlockNode blockNode);

  R visit(IfNode ifNode);

  R visit(SendNode sendNode);

  R visit(RecvNode recvNode);
}
