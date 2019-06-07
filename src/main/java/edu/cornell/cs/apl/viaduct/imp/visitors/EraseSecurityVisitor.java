package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.security.Label;

/** remove security from generated target code. */
public class EraseSecurityVisitor extends IdentityVisitor {
  public StmtNode run(StmtNode program) {
    return program.accept(this);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  @Override
  public StmtNode visit(DeclarationNode declNode) {
    return new DeclarationNode(declNode.getVariable(), Label.bottom());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    return new ArrayDeclarationNode(
        arrayDeclNode.getVariable(), arrayDeclNode.getLength(), Label.bottom());
  }
}
