package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;

/**
 * Skeletal implementation of the {@link ExprVisitor} interface.
 *
 * <p>See {@link AbstractReferenceVisitor} for a detailed explanation of available methods.
 *
 * @param <SelfT> the concrete implementation subclass
 * @param <ReferenceResultT> return type for reference nodes
 * @param <ExprResultT> return type for expression nodes
 */
public abstract class AbstractExprVisitor<
        SelfT extends AbstractExprVisitor<SelfT, ReferenceResultT, ExprResultT>,
        ReferenceResultT,
        ExprResultT>
    extends AbstractReferenceVisitor<SelfT, ReferenceResultT, ExprResultT>
    implements ExprVisitor<ExprResultT> {

  public final ExprResultT traverse(ExpressionNode node) {
    return node.accept(this);
  }

  /* ENTER  */

  protected abstract SelfT enter(ExpressionNode node);

  protected SelfT enter(LiteralNode node) {
    return enter((ExpressionNode) node);
  }

  protected SelfT enter(ReadNode node) {
    return enter((ExpressionNode) node);
  }

  protected SelfT enter(NotNode node) {
    return enter((ExpressionNode) node);
  }

  protected SelfT enter(BinaryExpressionNode node) {
    return enter((ExpressionNode) node);
  }

  protected SelfT enter(DowngradeNode node) {
    return enter((ExpressionNode) node);
  }

  /* LEAVE  */

  protected abstract ExprResultT leave(ExpressionNode node, SelfT visitor);

  protected ExprResultT leave(LiteralNode node, SelfT visitor) {
    return leave((ExpressionNode) node, visitor);
  }

  protected ExprResultT leave(ReadNode node, SelfT visitor, ReferenceResultT reference) {
    return leave((ExpressionNode) node, visitor);
  }

  protected ExprResultT leave(NotNode node, SelfT visitor, ExprResultT expression) {
    return leave((ExpressionNode) node, visitor);
  }

  protected ExprResultT leave(
      BinaryExpressionNode node, SelfT visitor, ExprResultT lhs, ExprResultT rhs) {
    return leave((ExpressionNode) node, visitor);
  }

  protected ExprResultT leave(DowngradeNode node, SelfT visitor, ExprResultT expression) {
    return leave((ExpressionNode) node, visitor);
  }

  /* VISIT  */

  @Override
  public ExprResultT visit(LiteralNode node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }

  @Override
  public ExprResultT visit(ReadNode node) {
    final SelfT visitor = enter(node);
    final ReferenceResultT reference = visitor.traverse(node.getReference());
    return leave(node, visitor, reference);
  }

  @Override
  public ExprResultT visit(NotNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT expression = visitor.traverse(node.getExpression());
    return leave(node, visitor, expression);
  }

  @Override
  public ExprResultT visit(BinaryExpressionNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT lhs = visitor.traverse(node.getLhs());
    final ExprResultT rhs = visitor.traverse(node.getRhs());
    return leave(node, visitor, lhs, rhs);
  }

  @Override
  public ExprResultT visit(DowngradeNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT expression = visitor.traverse(node.getExpression());
    return leave(node, visitor, expression);
  }
}
