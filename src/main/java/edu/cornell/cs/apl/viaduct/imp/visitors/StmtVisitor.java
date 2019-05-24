package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;

/** Statement visitor. */
public interface StmtVisitor<R> {
  R visit(SkipNode skipNode);

  R visit(DeclarationNode declarationNode);

  R visit(ArrayDeclarationNode arrayDeclarationNode);

  R visit(AssignNode assignNode);

  R visit(BlockNode blockNode);

  R visit(IfNode ifNode);

  R visit(SendNode sendNode);

  R visit(RecvNode recvNode);

  R visit(AnnotationNode annotNode);
}
