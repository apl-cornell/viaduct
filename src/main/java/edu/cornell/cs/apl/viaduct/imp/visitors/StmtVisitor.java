package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;

/** Statement visitor. */
public interface StmtVisitor<R> {
  R visit(VariableDeclarationNode variableDeclarationNode);

  R visit(ArrayDeclarationNode arrayDeclarationNode);

  R visit(LetBindingNode letBindingNode);

  R visit(AssignNode assignNode);

  R visit(SendNode sendNode);

  R visit(ReceiveNode receiveNode);

  R visit(IfNode ifNode);

  R visit(WhileNode whileNode);

  R visit(ForNode forNode);

  R visit(BlockNode blockNode);

  R visit(AssertNode assertNode);
}
