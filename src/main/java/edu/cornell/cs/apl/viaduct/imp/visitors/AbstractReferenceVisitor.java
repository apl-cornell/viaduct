package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

/**
 * Skeletal implementation of the {@link ReferenceVisitor} interface.
 *
 * <p>This class improves on the standard visitor interface in a couple of ways. First, traversing
 * the children is handled automatically. You only need to provide a way to combine the values
 * returned by traversing the children.
 *
 * <p>Second, the {@code visit(node)} methods are split into {@code enter(node)} and {@code
 * leave(node, visitor, children...)} methods. {@code enter} methods are executed before the
 * children of the node are traversed, and allow you to provide a different visitor while traversing
 * the children. This is useful, for example, for scope management: you can create a new visitor
 * that maintains its own context information (copied from the parent visitor), and the context will
 * be reverted once we switch back to the parent visitor.
 *
 * <p>Third, this class supports inheritance. {@code enter} and {@code leave} methods by default
 * delegate to {@link #enter(ReferenceNode)} and {@link #leave(ReferenceNode, SelfT)} methods. So,
 * for uniform functionality, you may only need to override as little as a single methods.
 *
 * @param <SelfT> concrete implementation subclass
 * @param <ReferenceResultT> return type for reference nodes
 * @param <ExprResultT> return type for expression nodes
 */
public abstract class AbstractReferenceVisitor<
        SelfT extends AbstractReferenceVisitor<SelfT, ReferenceResultT, ExprResultT>,
        ReferenceResultT,
        ExprResultT>
    implements ReferenceVisitor<ReferenceResultT> {

  /** Return the visitor that will be used for expression sub-nodes. */
  protected abstract ExprVisitor<ExprResultT> getExpressionVisitor();

  /* ENTER  */

  protected abstract SelfT enter(ReferenceNode node);

  protected SelfT enter(Variable node) {
    return enter((ReferenceNode) node);
  }

  protected SelfT enter(ArrayIndexingNode node) {
    return enter((ReferenceNode) node);
  }

  /* LEAVE  */

  protected ReferenceResultT leave(ReferenceNode node, SelfT visitor) {
    throw new MissingCaseError(node);
  }

  protected ReferenceResultT leave(Variable node, SelfT visitor) {
    return leave((ReferenceNode) node, visitor);
  }

  protected ReferenceResultT leave(
      ArrayIndexingNode node, SelfT visitor, ReferenceResultT array, ExprResultT index) {
    return leave((ReferenceNode) node, visitor);
  }

  /* VISIT  */

  @Override
  public ReferenceResultT visit(Variable node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }

  @Override
  public ReferenceResultT visit(ArrayIndexingNode node) {
    final SelfT visitor = enter(node);
    final ReferenceResultT array = node.getArray().accept(visitor);
    final ExprResultT index = node.getIndex().accept(visitor.getExpressionVisitor());
    return leave(node, visitor, array, index);
  }
}
