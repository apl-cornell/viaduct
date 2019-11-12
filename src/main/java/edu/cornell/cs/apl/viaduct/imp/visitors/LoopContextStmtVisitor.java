package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.errors.JumpWithoutLoopScopeError;
import edu.cornell.cs.apl.viaduct.errors.NameClashError;
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.JumpLabel;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a visitor similar to {@link ContextStmtVisitor}, but additionally maintains a mapping
 * from jump labels (that correspond to enclosing loops) to some data. Subclasses need to provide
 * {@code extract(loopNode)} methods that compute the information to store for the loop. This
 * information is then passed back to the subclass at specific points that make sense (there is no
 * {@code get} method). For example, {@link #enterBody(LoopNode, Object)} and {@link
 * #leave(BreakNode, LoopContextStmtVisitor, Object)}.
 *
 * <p>The {@code extract} methods are guaranteed to be called exactly once per loop node, right
 * after the respective {@code enter} method.
 */
public abstract class LoopContextStmtVisitor<
        SelfT extends
            LoopContextStmtVisitor<
                    SelfT, ContextValueT, LoopValueT, ReferenceResultT, ExprResultT, StmtResultT>,
        ContextValueT,
        LoopValueT,
        ReferenceResultT,
        ExprResultT,
        StmtResultT>
    extends ContextStmtVisitor<SelfT, ContextValueT, ReferenceResultT, ExprResultT, StmtResultT> {

  private Map<JumpLabel, LoopValueT> labelContext;

  /**
   * The last label to be added to the context. Useful for things like plain {@code break}
   * statements that do not explicitly name a loop.
   */
  private Option<LoopValueT> lastLoopValue;

  /** Construct a visitor with empty jump label context. */
  protected LoopContextStmtVisitor() {
    super();
    this.labelContext = HashMap.empty();
    this.lastLoopValue = Option.none();
  }

  /**
   * Construct a new visitor where the context is copied from the given visitor.
   *
   * <p>Useful for implementing {@link #newScope()}.
   *
   * @param visitor instance to clone the context from
   */
  protected LoopContextStmtVisitor(
      LoopContextStmtVisitor<
              SelfT, ContextValueT, LoopValueT, ReferenceResultT, ExprResultT, StmtResultT>
          visitor) {
    super(visitor);
    this.labelContext = visitor.labelContext;
    this.lastLoopValue = visitor.lastLoopValue;
  }

  protected abstract LoopValueT extract(WhileNode node);

  protected abstract LoopValueT extract(ForNode node);

  protected abstract LoopValueT extract(LoopNode node);

  protected SelfT enterBody(WhileNode node, LoopValueT loopValue, ExprResultT guard) {
    return enter((StatementNode) node);
  }

  protected SelfT enterBody(
      ForNode node,
      LoopValueT loopValue,
      Iterable<StmtResultT> initialize,
      ExprResultT guard,
      Iterable<StmtResultT> update) {
    return enter((StatementNode) node);
  }

  protected SelfT enterBody(LoopNode node, LoopValueT loopValue) {
    return enter((StatementNode) node);
  }

  @Override
  protected final StmtResultT leave(BreakNode node, SelfT visitor) {
    // This will never be called.
    throw new UnsupportedOperationException();
  }

  /**
   * Called when we are done traversing a break statement.
   *
   * @param node original node
   * @param visitor visitor instance used for children nodes
   * @param loopValue value extracted for the loop this statement will break out of
   */
  protected abstract StmtResultT leave(BreakNode node, SelfT visitor, LoopValueT loopValue);

  @Override
  public final StmtResultT visit(WhileNode node) {
    final SelfT visitor = enter(node);
    final LoopValueT loopValue = extract(node);

    final ExprResultT guard = node.getGuard().accept(visitor.getExpressionVisitor());

    // Traverse body
    final SelfT bodyVisitor1 = visitor.newScope();
    bodyVisitor1.put(node.getJumpLabel(), loopValue);
    final SelfT bodyVisitor2 = bodyVisitor1.enterBody(node, loopValue, guard);
    final StmtResultT body = node.getBody().accept(bodyVisitor2);

    return leave(node, visitor, guard, body);
  }

  @Override
  public StmtResultT visit(ForNode node) {
    final SelfT visitor = enter(node);
    final LoopValueT loopValue = extract(node);

    final List<StmtResultT> initialize = new ArrayList<>();
    for (StatementNode initStmt : node.getInitialize()) {
      initialize.add(initStmt.accept(visitor));
    }

    final ExprResultT guard = node.getGuard().accept(visitor.getExpressionVisitor());

    final List<StmtResultT> update = new ArrayList<>();
    for (StatementNode updateStmt : node.getUpdate()) {
      update.add(updateStmt.accept(visitor));
    }

    // Traverse body
    final SelfT bodyVisitor1 = visitor.newScope();
    bodyVisitor1.put(node.getJumpLabel(), loopValue);
    final SelfT bodyVisitor2 = bodyVisitor1.enterBody(node, loopValue, initialize, guard, update);
    final StmtResultT body = node.getBody().accept(bodyVisitor2);

    return leave(node, visitor, initialize, guard, update, body);
  }

  @Override
  public StmtResultT visit(LoopNode node) {
    final SelfT visitor = enter(node);
    final LoopValueT loopValue = extract(node);

    // Traverse body
    final SelfT bodyVisitor1 = visitor.newScope();
    bodyVisitor1.put(node.getJumpLabel(), loopValue);
    final SelfT bodyVisitor2 = bodyVisitor1.enterBody(node, loopValue);
    final StmtResultT body = node.getBody().accept(bodyVisitor2);

    return leave(node, visitor, body);
  }

  @Override
  public final StmtResultT visit(BreakNode node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor, get(node, node.getJumpLabel()));
  }

  /** Get the value attached to the loop label. */
  private LoopValueT get(HasLocation node, JumpLabel label) {
    if (label == null) {
      return lastLoopValue.getOrElseThrow(() -> new JumpWithoutLoopScopeError(node));
    } else {
      return labelContext.get(label).getOrElseThrow(() -> new UndefinedNameError(label));
    }
  }

  /** Set the value attached to a loop label. */
  final void put(JumpLabel label, LoopValueT value) {
    if (label != null && labelContext.containsKey(label)) {
      final HasLocation previousDeclaration = labelContext.keySet().find(label::equals).getOrNull();
      throw new NameClashError(previousDeclaration, label);

    }
    labelContext = labelContext.put(label, value);
    lastLoopValue = Option.some(value);
  }
}
