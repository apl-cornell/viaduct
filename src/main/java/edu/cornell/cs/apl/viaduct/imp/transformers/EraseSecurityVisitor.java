package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

// TODO: remove this as this is unnecessary

/** Remove labels from generated target code. */
class EraseSecurityVisitor extends IdentityProgramVisitor {
  private final ExprVisitor<ExpressionNode> expressionVisitor = new EraseExprVisitor();
  private final StmtVisitor<StatementNode> statementVisitor = new EraseStmtVisitor();

  @Override
  protected ExprVisitor<ExpressionNode> getExpressionVisitor() {
    return expressionVisitor;
  }

  @Override
  protected StmtVisitor<StatementNode> getStatementVisitor() {
    return statementVisitor;
  }

  protected class EraseExprVisitor extends IdentityExprVisitor {
    @Override
    protected ExpressionNode leave(
        DowngradeNode node, IdentityExprVisitor visitor, ExpressionNode expression) {
      return expression;
    }
  }

  protected class EraseStmtVisitor extends IdentityStmtVisitor {
    @Override
    protected StatementNode leave(VariableDeclarationNode node, IdentityStmtVisitor visitor) {
      return node.toBuilder().setLabel(null).build();
    }

    @Override
    protected StatementNode leave(
        ArrayDeclarationNode node, IdentityStmtVisitor visitor, ExpressionNode length) {
      return node.toBuilder().setLabel(null).build();
    }
  }
}
