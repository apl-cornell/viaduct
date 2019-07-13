package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.security.Label;

/** remove security from generated target code. */
public class EraseSecurityVisitor extends IdentityVisitor {
  @Override
  public StmtNode run(StmtNode program) {
    return program.accept(this);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public StmtNode visit(VariableDeclarationNode declNode) {
    return VariableDeclarationNode.create(
        declNode.getVariable(), declNode.getType(), Label.bottom());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    return ArrayDeclarationNode.create(
        arrayDeclNode.getVariable(), arrayDeclNode.getLength(),
        arrayDeclNode.getType(), Label.bottom());
  }
}
