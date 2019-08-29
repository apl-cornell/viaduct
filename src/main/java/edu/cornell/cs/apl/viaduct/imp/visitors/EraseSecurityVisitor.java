package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;

/** remove security from generated target code. */
public class EraseSecurityVisitor extends IdentityVisitor {
  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  /*
  @Override
  public StatementNode visit(VariableDeclarationNode declNode) {
    return VariableDeclarationNode.create(
        declNode.getVariable(), declNode.getType(), Label.bottom());
  }

  @Override
  public StatementNode visit(ArrayDeclarationNode arrayDeclNode) {
    return ArrayDeclarationNode.create(
        arrayDeclNode.getVariable(), arrayDeclNode.getLength(),
        arrayDeclNode.getType(), Label.bottom());
  }
  */
}
