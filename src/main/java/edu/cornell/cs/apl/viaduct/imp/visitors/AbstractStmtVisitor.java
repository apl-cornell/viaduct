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
import java.util.LinkedList;
import java.util.List;

/**
 * Skeletal implementation of the {@link StmtVisitor} interface.
 *
 * <p>See {@link AbstractReferenceVisitor} for a detailed explanation of available methods.
 *
 * @param <SelfT> the concrete implementation subclass
 * @param <ReferenceResultT> return type for reference nodes
 * @param <ExprResultT> return type for expression nodes
 * @param <StmtResultT> return type for statement nodes
 */
public abstract class AbstractStmtVisitor<
        SelfT extends AbstractStmtVisitor<SelfT, ReferenceResultT, ExprResultT, StmtResultT>,
        ReferenceResultT,
        ExprResultT,
        StmtResultT>
    extends AbstractExprVisitor<SelfT, ReferenceResultT, ExprResultT>
    implements StmtVisitor<StmtResultT> {

  public final StmtResultT traverse(StatementNode node) {
    return node.accept(this);
  }

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

  protected abstract StmtResultT leave(StatementNode node, SelfT visitor);

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

  protected StmtResultT leave(ReceiveNode node, SelfT visitor) {
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
      StmtResultT initialize,
      ExprResultT guard,
      StmtResultT update,
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
    final ExprResultT length = visitor.traverse(node.getLength());
    return leave(node, visitor, length);
  }

  @Override
  public StmtResultT visit(LetBindingNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT rhs = visitor.traverse(node.getRhs());
    return leave(node, visitor, rhs);
  }

  @Override
  public StmtResultT visit(AssignNode node) {
    final SelfT visitor = enter(node);
    final ReferenceResultT lhs = visitor.traverse(node.getLhs());
    final ExprResultT rhs = visitor.traverse(node.getRhs());
    return leave(node, visitor, lhs, rhs);
  }

  @Override
  public StmtResultT visit(SendNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT sentExpression = visitor.traverse(node.getSentExpression());
    return leave(node, visitor, sentExpression);
  }

  @Override
  public StmtResultT visit(ReceiveNode node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }

  @Override
  public StmtResultT visit(IfNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT guard = visitor.traverse(node.getGuard());
    final StmtResultT thenBranch = visitor.traverse(node.getThenBranch());
    final StmtResultT elseBranch = visitor.traverse(node.getElseBranch());
    return leave(node, visitor, guard, thenBranch, elseBranch);
  }

  @Override
  public StmtResultT visit(WhileNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT guard = visitor.traverse(node.getGuard());
    final StmtResultT body = visitor.traverse(node.getBody());
    return leave(node, visitor, guard, body);
  }

  @Override
  public StmtResultT visit(ForNode node) {
    final SelfT visitor = enter(node);
    final StmtResultT initialize = visitor.traverse(node.getInitialize());
    final ExprResultT guard = visitor.traverse(node.getGuard());
    final StmtResultT update = visitor.traverse(node.getUpdate());
    final StmtResultT body = visitor.traverse(node.getBody());
    return leave(node, visitor, initialize, guard, update, body);
  }

  @Override
  public StmtResultT visit(LoopNode node) {
    final SelfT visitor = enter(node);
    final StmtResultT body = visitor.traverse(node.getBody());
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
      statements.add(visitor.traverse(stmt));
    }
    return leave(node, visitor, statements);
  }

  @Override
  public StmtResultT visit(AssertNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT expression = visitor.traverse(node.getExpression());
    return leave(node, visitor, expression);
  }
}
