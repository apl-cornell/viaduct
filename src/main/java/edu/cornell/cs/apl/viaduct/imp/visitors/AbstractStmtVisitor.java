package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Skeletal implementation of the {@link StmtVisitor} interface.
 *
 * <p>See {@link AbstractReferenceVisitor} for a detailed explanation of available methods.
 *
 * @param <SelfT> concrete implementation subclass
 * @param <ReferenceResultT> return type for reference nodes
 * @param <ExprResultT> return type for expression nodes
 * @param <StmtResultT> return type for statement nodes
 */
public abstract class AbstractStmtVisitor<
        SelfT extends AbstractStmtVisitor<SelfT, ReferenceResultT, ExprResultT, StmtResultT>,
        ReferenceResultT,
        ExprResultT,
        StmtResultT>
    implements StmtVisitor<StmtResultT> {

  /** Return the visitor that will be used for reference sub-nodes. */
  protected abstract ReferenceVisitor<ReferenceResultT> getReferenceVisitor();

  /** Return the visitor that will be used for expression sub-nodes. */
  protected abstract ExprVisitor<ExprResultT> getExpressionVisitor();

  /* ENTER  */

  protected abstract SelfT enter(StatementNode node);

  protected SelfT enter(VariableDeclarationNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(ArrayDeclarationNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(LetBindingNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(AssignNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(SendNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(ReceiveNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(IfNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(WhileNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(ForNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(LoopNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(BreakNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(BlockNode node) {
    return enter((StatementNode) node);
  }

  protected SelfT enter(AssertNode node) {
    return enter((StatementNode) node);
  }

  /* LEAVE  */

  protected StmtResultT leave(StatementNode node, SelfT visitor) {
    throw new MissingCaseError(node);
  }

  protected StmtResultT leave(VariableDeclarationNode node, SelfT visitor) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(ArrayDeclarationNode node, SelfT visitor, ExprResultT length) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(LetBindingNode node, SelfT visitor, ExprResultT rhs) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(
      AssignNode node, SelfT visitor, ReferenceResultT lhs, ExprResultT rhs) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(SendNode node, SelfT visitor, ExprResultT sentExpression) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(ReceiveNode node, SelfT visitor, ReferenceResultT lhs) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(
      IfNode node,
      SelfT visitor,
      ExprResultT guard,
      StmtResultT thenBranch,
      StmtResultT elseBranch) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(WhileNode node, SelfT visitor, ExprResultT guard, StmtResultT body) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(
      ForNode node,
      SelfT visitor,
      Iterable<StmtResultT> initialize,
      ExprResultT guard,
      Iterable<StmtResultT> update,
      StmtResultT body) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(LoopNode node, SelfT visitor, StmtResultT body) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(BreakNode node, SelfT visitor) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(BlockNode node, SelfT visitor, Iterable<StmtResultT> statements) {
    return leave((StatementNode) node, visitor);
  }

  protected StmtResultT leave(AssertNode node, SelfT visitor, ExprResultT expression) {
    return leave((StatementNode) node, visitor);
  }

  /* VISIT  */

  @Override
  public StmtResultT visit(VariableDeclarationNode node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }

  @Override
  public StmtResultT visit(ArrayDeclarationNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT length = node.getLength().accept(visitor.getExpressionVisitor());
    return leave(node, visitor, length);
  }

  @Override
  public StmtResultT visit(LetBindingNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT rhs = node.getRhs().accept(visitor.getExpressionVisitor());
    return leave(node, visitor, rhs);
  }

  @Override
  public StmtResultT visit(AssignNode node) {
    final SelfT visitor = enter(node);
    final ReferenceResultT lhs = node.getLhs().accept(visitor.getReferenceVisitor());
    final ExprResultT rhs = node.getRhs().accept(visitor.getExpressionVisitor());
    return leave(node, visitor, lhs, rhs);
  }

  @Override
  public StmtResultT visit(SendNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT sentExpression =
        node.getSentExpression().accept(visitor.getExpressionVisitor());
    return leave(node, visitor, sentExpression);
  }

  @Override
  public StmtResultT visit(ReceiveNode node) {
    final SelfT visitor = enter(node);
    final ReferenceResultT lhs = node.getVariable().accept(visitor.getReferenceVisitor());
    return leave(node, visitor, lhs);
  }

  @Override
  public StmtResultT visit(IfNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT guard = node.getGuard().accept(visitor.getExpressionVisitor());
    final StmtResultT thenBranch = node.getThenBranch().accept(visitor);
    final StmtResultT elseBranch = node.getElseBranch().accept(visitor);
    return leave(node, visitor, guard, thenBranch, elseBranch);
  }

  @Override
  public StmtResultT visit(WhileNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT guard = node.getGuard().accept(visitor.getExpressionVisitor());
    final StmtResultT body = node.getBody().accept(visitor);
    return leave(node, visitor, guard, body);
  }

  @Override
  public StmtResultT visit(ForNode node) {
    final SelfT visitor = enter(node);
    final List<StmtResultT> initialize = new ArrayList<>();
    for (StatementNode initStmt : node.getInitialize()) {
      initialize.add(initStmt.accept(visitor));
    }

    final ExprResultT guard = node.getGuard().accept(visitor.getExpressionVisitor());

    final List<StmtResultT> update = new ArrayList<>();
    for (StatementNode updateStmt : node.getUpdate()) {
      update.add(updateStmt.accept(visitor));
    }

    final StmtResultT body = node.getBody().accept(visitor);
    return leave(node, visitor, initialize, guard, update, body);
  }

  @Override
  public StmtResultT visit(LoopNode node) {
    final SelfT visitor = enter(node);
    final StmtResultT body = node.getBody().accept(visitor);
    return leave(node, visitor, body);
  }

  @Override
  public StmtResultT visit(BreakNode node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }

  @Override
  public StmtResultT visit(BlockNode node) {
    final SelfT visitor = enter(node);
    final List<StmtResultT> statements = new LinkedList<>();
    for (StatementNode stmt : node) {
      statements.add(stmt.accept(visitor));
    }
    return leave(node, visitor, statements);
  }

  @Override
  public StmtResultT visit(AssertNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT expression = node.getExpression().accept(visitor.getExpressionVisitor());
    return leave(node, visitor, expression);
  }
}
