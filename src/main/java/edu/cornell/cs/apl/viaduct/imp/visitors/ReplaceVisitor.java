package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import java.util.HashMap;
import java.util.Map;

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

    protected ExpressionNode replace(ExpressionNode node, ExpressionNode newNode) {
      final ExpressionNode replacement = exprReplacements.get(node);
      return replacement != null ? replacement : newNode;
    }

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

    @Override
    protected ExpressionNode leave(
        ReadNode node, ReplaceExprVisitor visitor, ReferenceNode reference) {
      return replace(node, node.toBuilder().setReference(reference).build());
    }

    @Override
    protected ExpressionNode leave(
        NotNode node, ReplaceExprVisitor visitor, ExpressionNode expression) {
      return replace(node, node.toBuilder().setExpression(expression).build());
    }

    @Override
    protected ExpressionNode leave(
        BinaryExpressionNode node,
        ReplaceExprVisitor visitor,
        ExpressionNode lhs,
        ExpressionNode rhs) {
      return replace(node, node.toBuilder().setLhs(lhs).setRhs(rhs).build());
    }

    @Override
    protected ExpressionNode leave(
        DowngradeNode node, ReplaceExprVisitor visitor, ExpressionNode expression) {
      return replace(node, node.toBuilder().setExpression(expression).build());
    }
  }

  protected class ReplaceStmtVisitor
      extends AbstractStmtVisitor<
          ReplaceStmtVisitor, ReferenceNode, ExpressionNode, StatementNode> {

    protected StatementNode replace(StatementNode node, StatementNode newNode) {
      final StatementNode replacement = stmtReplacements.get(node);
      return replacement != null ? replacement : newNode;
    }

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

    /*
    @Override
    protected StatementNode leave(StatementNode node, ReplaceStmtVisitor visitor) {
      final StatementNode replacement = stmtReplacements.get(node);
      return replacement != null ? replacement : node;
    }
    */

    @Override
    protected StatementNode leave(VariableDeclarationNode node, ReplaceStmtVisitor visitor) {
      return replace(node, node.toBuilder().build());
    }

    @Override
    protected StatementNode leave(
        ArrayDeclarationNode node, ReplaceStmtVisitor visitor, ExpressionNode length) {
      return replace(node, node.toBuilder().setLength(length).build());
    }

    @Override
    protected StatementNode leave(
        LetBindingNode node, ReplaceStmtVisitor visitor, ExpressionNode rhs) {
      return replace(node, node.toBuilder().setRhs(rhs).build());
    }

    @Override
    protected StatementNode leave(
        AssignNode node, ReplaceStmtVisitor visitor, ReferenceNode lhs, ExpressionNode rhs) {
      return replace(node, node.toBuilder().setLhs(lhs).setRhs(rhs).build());
    }

    @Override
    protected StatementNode leave(
        SendNode node, ReplaceStmtVisitor visitor, ExpressionNode sentExpression) {
      return replace(node, node.toBuilder().setSentExpression(sentExpression).build());
    }

    @Override
    protected StatementNode leave(ReceiveNode node, ReplaceStmtVisitor visitor, ReferenceNode lhs) {
      return replace(node, node.toBuilder().setVariable((Variable) lhs).build());
    }

    @Override
    protected StatementNode leave(
        IfNode node,
        ReplaceStmtVisitor visitor,
        ExpressionNode guard,
        StatementNode thenBranch,
        StatementNode elseBranch) {
      return replace(
          node,
          node.toBuilder()
              .setGuard(guard)
              .setThenBranch((BlockNode) thenBranch)
              .setElseBranch((BlockNode) elseBranch)
              .setLoopGuard(node.isLoopGuard())
              .build());
    }

    @Override
    protected StatementNode leave(
        WhileNode node, ReplaceStmtVisitor visitor, ExpressionNode guard, StatementNode body) {
      return replace(node, node.toBuilder().setGuard(guard).setBody((BlockNode) body).build());
    }

    @Override
    protected StatementNode leave(
        ForNode node,
        ReplaceStmtVisitor visitor,
        Iterable<StatementNode> initialize,
        ExpressionNode guard,
        Iterable<StatementNode> update,
        StatementNode body) {
      return replace(
          node,
          node.toBuilder()
              .setInitialize(initialize)
              .setGuard(guard)
              .setUpdate(update)
              .setBody((BlockNode) body)
              .build());
    }

    @Override
    protected StatementNode leave(LoopNode node, ReplaceStmtVisitor visitor, StatementNode body) {
      return replace(node, node.toBuilder().setBody((BlockNode) body).build());
    }

    @Override
    protected StatementNode leave(BreakNode node, ReplaceStmtVisitor visitor) {
      return replace(node, node.toBuilder().build());
    }

    @Override
    protected StatementNode leave(
        BlockNode node, ReplaceStmtVisitor visitor, Iterable<StatementNode> statements) {
      return replace(node, node.toBuilder().setStatements(statements).build());
    }

    @Override
    protected StatementNode leave(
        AssertNode node, ReplaceStmtVisitor visitor, ExpressionNode expression) {
      return replace(node, node.toBuilder().setExpression(expression).build());
    }
  }
}
