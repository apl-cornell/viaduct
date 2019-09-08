package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import java.util.HashMap;
import java.util.Map;

// TODO: every place this is useful, there is a better way of doing this. Remove.

/** Find and replace for AST nodes. */
public class ReplaceVisitor extends IdentityProgramVisitor {
  protected final Map<StatementNode, StatementNode> stmtReplacements;
  protected final Map<ExpressionNode, ExpressionNode> exprReplacements;

  private final ExprVisitor<ExpressionNode> expressionVisitor = new ReplaceExprVisitor();
  private final StmtVisitor<StatementNode> statementVisitor = new ReplaceStmtVisitor();

  public ReplaceVisitor() {
    this.stmtReplacements = new HashMap<>();
    this.exprReplacements = new HashMap<>();
  }

  public ReplaceVisitor(
      Map<ExpressionNode, ExpressionNode> expressionReplacements,
      Map<StatementNode, StatementNode> statementReplacements) {
    this.exprReplacements = expressionReplacements;
    this.stmtReplacements = statementReplacements;
  }

  @Override
  protected ExprVisitor<ExpressionNode> getExpressionVisitor() {
    return expressionVisitor;
  }

  @Override
  protected StmtVisitor<StatementNode> getStatementVisitor() {
    return statementVisitor;
  }

  protected class ReplaceExprVisitor
      extends AbstractExprVisitor<ReplaceExprVisitor, ReferenceNode, ExpressionNode> {

    @Override
    protected ReferenceVisitor<ReferenceNode> getReferenceVisitor() {
      return ReplaceVisitor.this.getReferenceVisitor();
    }

    @Override
    protected ReplaceExprVisitor enter(ExpressionNode node) {
      return this;
    }

    @Override
    protected ExpressionNode leave(ExpressionNode node, ReplaceExprVisitor visitor) {
      final ExpressionNode replacement = exprReplacements.get(node);
      return replacement != null ? replacement : node;
    }
  }

  protected class ReplaceStmtVisitor
      extends AbstractStmtVisitor<
          ReplaceStmtVisitor, ReferenceNode, ExpressionNode, StatementNode> {

    @Override
    protected ReferenceVisitor<ReferenceNode> getReferenceVisitor() {
      return ReplaceVisitor.this.getReferenceVisitor();
    }

    @Override
    protected ExprVisitor<ExpressionNode> getExpressionVisitor() {
      return ReplaceVisitor.this.getExpressionVisitor();
    }

    @Override
    protected ReplaceStmtVisitor enter(StatementNode node) {
      return this;
    }

    @Override
    protected StatementNode leave(StatementNode node, ReplaceStmtVisitor visitor) {
      final StatementNode replacement = stmtReplacements.get(node);
      return replacement != null ? replacement : node;
    }
  }
}
