package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

/**
 * Skeletal implementation of the {@link ReferenceVisitor} interface.
 *
 * <p>This class improves on the standard visitor interface in a couple of ways. First, traversing
 * the children is handled automatically. You only need to provide a way to combine the values
 * returned by traversing the children.
 *
 * <p>Second, the {@code visit(node)} methods are split into {@code enter(node)} and {@code
 * leave(node, visitor, children...)} methods. {@code enter(node)} methods are executed before the
 * children of the node are traversed, and allow you to provide a different visitor while traversing
 * the children. This is useful, for example, for scope management: you can create a new visitor
 * that maintains its own context information (copied from the parent visitor), and the context will
 * be reverted once we switch back to using the parent visitor.
 *
 * <p>Third, this class supports inheritance. {@code enter(node)} and {@code leave(...)} methods by
 * default delegate to {@link #enter(Reference)} and {@link #leave(Reference, SelfT)} methods. So
 * for uniform functionality, you may only need to override as little as a single methods.
 *
 * @param <SelfT> the concrete implementation subclass
 * @param <ReferenceResultT> return type for reference nodes
 * @param <ExprResultT> return type for expression nodes
 */
public abstract class AbstractReferenceVisitor<
        SelfT extends AbstractExprVisitor<SelfT, ReferenceResultT, ExprResultT>,
        ReferenceResultT,
        ExprResultT>
    implements ReferenceVisitor<ReferenceResultT> {

  public final ReferenceResultT traverse(Reference node) {
    return node.accept(this);
  }

  /* ENTER  */

  protected abstract SelfT enter(Reference node);

  protected SelfT enter(Variable node) {
    return enter((Reference) node);
  }

  protected SelfT enter(ArrayIndex node) {
    return enter((Reference) node);
  }

  /* LEAVE  */

  protected abstract ReferenceResultT leave(Reference node, SelfT visitor);

  protected ReferenceResultT leave(Variable node, SelfT visitor) {
    return leave((Reference) node, visitor);
  }

  protected ReferenceResultT leave(
      ArrayIndex node, SelfT visitor, ReferenceResultT array, ExprResultT index) {
    return leave((Reference) node, visitor);
  }

  /* VISIT  */

  @Override
  public ReferenceResultT visit(Variable node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }

  @Override
  public ReferenceResultT visit(ArrayIndex node) {
    final SelfT visitor = enter(node);
    final ReferenceResultT array = visitor.traverse(node.getArray());
    final ExprResultT index = visitor.traverse(node.getIndex());
    return leave(node, visitor, array, index);
  }
}
