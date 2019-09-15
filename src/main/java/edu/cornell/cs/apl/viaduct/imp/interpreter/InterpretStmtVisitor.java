package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.errors.AssertionFailureError;
import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.errors.ImpArrayOutOfBoundsError;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.JumpLabel;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ContextStmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class InterpretStmtVisitor
    extends ContextStmtVisitor<InterpretStmtVisitor, AllocatedObject, Reference, ImpValue, Void> {
  /** The process to execute the statement as. */
  private final @Nullable ProcessName processName;

  /** The (multi-way) channel that connects {@code processName} to all other processes. */
  private final @Nullable Channel<ImpValue> channel;

  private final InterpretReferenceVisitor referenceVisitor = new InterpretReferenceVisitor();
  private final InterpretExprVisitor expressionVisitor = new InterpretExprVisitor();

  /**
   * Create a new interpreter that acts as {@code processName} and uses {@code channel} to
   * communicate.
   *
   * @param process the process to run the statements as
   * @param channel connects {@code processName} to all other hosts involved in the computation
   */
  InterpretStmtVisitor(@Nonnull ProcessName process, @Nonnull Channel<ImpValue> channel) {
    this.processName = Objects.requireNonNull(process);
    this.channel = Objects.requireNonNull(channel);
  }

  /**
   * Create a new interpreter that can only evaluate statements that do not send or receive values.
   */
  InterpretStmtVisitor() {
    this.processName = null;
    this.channel = null;
  }

  /** Create a clone of the given visitor. */
  private InterpretStmtVisitor(InterpretStmtVisitor that) {
    super(that);
    this.processName = that.processName;
    this.channel = that.channel;
  }

  /** Get the current variable store. */
  public Store getStore() {
    return Store.create(getContext());
  }

  @Override
  protected ReferenceVisitor<Reference> getReferenceVisitor() {
    return referenceVisitor;
  }

  @Override
  public ExprVisitor<ImpValue> getExpressionVisitor() {
    return expressionVisitor;
  }

  @Override
  protected AllocatedObject extract(VariableDeclarationNode node) {
    return ValueReference.allocate();
  }

  @Override
  protected AllocatedObject extract(ArrayDeclarationNode node, ImpValue length) {
    return ArrayReference.allocate(((IntegerValue) length).getValue());
  }

  @Override
  protected AllocatedObject extract(LetBindingNode node, ImpValue rhs) {
    return ValueReference.allocate(rhs);
  }

  @Override
  protected InterpretStmtVisitor newScope() {
    return new InterpretStmtVisitor(this);
  }

  @Override
  protected InterpretStmtVisitor enter(StatementNode node) {
    return this;
  }

  @Override
  protected Void leave(VariableDeclarationNode node, InterpretStmtVisitor visitor) {
    return null;
  }

  @Override
  protected Void leave(ArrayDeclarationNode node, InterpretStmtVisitor visitor, ImpValue length) {
    return null;
  }

  @Override
  protected Void leave(LetBindingNode node, InterpretStmtVisitor visitor, ImpValue rhs) {
    return null;
  }

  @Override
  protected Void leave(AssignNode node, InterpretStmtVisitor visitor, Reference lhs, ImpValue rhs) {
    lhs.set(rhs);
    return null;
  }

  @Override
  protected Void leave(SendNode node, InterpretStmtVisitor visitor, ImpValue sentValue) {
    if (processName == null || channel == null) {
      throw new NullPointerException("Unexpected send in a local statement.");
    } else {
      this.channel.send(this.processName, node.getRecipient(), sentValue);
      return null;
    }
  }

  @Override
  protected Void leave(ReceiveNode node, InterpretStmtVisitor visitor, Reference lhs) {
    if (processName == null || channel == null) {
      throw new NullPointerException("Unexpected receive in a local statement.");
    } else {
      final ImpValue value = this.channel.receive(node.getSender(), this.processName);
      lhs.set(value);
      return null;
    }
  }

  @Override
  protected Void leave(BreakNode node, InterpretStmtVisitor visitor) {
    throw new BreakSignal(node.getJumpLabel());
  }

  @Override
  protected Void leave(BlockNode node, InterpretStmtVisitor visitor, Iterable<Void> statements) {
    return null;
  }

  @Override
  protected Void leave(AssertNode node, InterpretStmtVisitor visitor, ImpValue expressionValue) {
    if (!((BooleanValue) expressionValue).getValue()) {
      throw new AssertionFailureError(node);
    }
    return null;
  }

  // NOTE: We need to implement control structures by overriding {@code visit} methods since the
  // default traversal would execute all branches.

  @Override
  public Void visit(IfNode node) {
    final InterpretStmtVisitor visitor = enter(node);
    final ImpValue guard = node.getGuard().accept(visitor.getExpressionVisitor());
    if (((BooleanValue) guard).getValue()) {
      return node.getThenBranch().accept(visitor);
    } else {
      return node.getElseBranch().accept(visitor);
    }
  }

  @Override
  public Void visit(WhileNode node) {
    final InterpretStmtVisitor visitor = enter(node);

    try {
      while (((BooleanValue) node.getGuard().accept(visitor.getExpressionVisitor())).getValue()) {
        node.getBody().accept(visitor);
      }
    } catch (BreakSignal e) {
      maybePropagate(e, node.getJumpLabel());
    }

    return null;
  }

  @Override
  public Void visit(ForNode node) {
    throw new ElaborationException();
  }

  @Override
  public Void visit(LoopNode node) {
    final InterpretStmtVisitor visitor = enter(node);

    try {
      //noinspection InfiniteLoopStatement
      while (true) {
        node.getBody().accept(visitor);
      }
    } catch (BreakSignal e) {
      maybePropagate(e, node.getJumpLabel());
    }

    return null;
  }

  /** Propagate the break signal if it is intended for an outer loop. */
  private void maybePropagate(BreakSignal signal, JumpLabel loopLabel) throws BreakSignal {
    if (signal.getLabel() != null && !signal.getLabel().equals(loopLabel)) {
      throw signal;
    }
  }

  private final class InterpretReferenceVisitor
      extends AbstractReferenceVisitor<InterpretReferenceVisitor, Reference, ImpValue> {

    @Override
    protected ExprVisitor<ImpValue> getExpressionVisitor() {
      return InterpretStmtVisitor.this.getExpressionVisitor();
    }

    @Override
    protected InterpretReferenceVisitor enter(ReferenceNode node) {
      return this;
    }

    @Override
    protected Reference leave(Variable node, InterpretReferenceVisitor visitor) {
      return InterpretStmtVisitor.this.get(node);
    }

    @Override
    protected Reference leave(
        ArrayIndexingNode node,
        InterpretReferenceVisitor visitor,
        Reference array,
        ImpValue index) {
      final int indexValue = ((IntegerValue) index).getValue();
      try {
        return array.index(indexValue);
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new ImpArrayOutOfBoundsError(node, array.size(), indexValue);
      }
    }
  }

  private final class InterpretExprVisitor
      extends AbstractExprVisitor<InterpretExprVisitor, Reference, ImpValue> {

    @Override
    protected ReferenceVisitor<Reference> getReferenceVisitor() {
      return InterpretStmtVisitor.this.getReferenceVisitor();
    }

    @Override
    protected InterpretExprVisitor enter(ExpressionNode node) {
      return this;
    }

    @Override
    protected ImpValue leave(LiteralNode node, InterpretExprVisitor visitor) {
      return node.getValue();
    }

    @Override
    protected ImpValue leave(ReadNode node, InterpretExprVisitor visitor, Reference reference) {
      return reference.get();
    }

    @Override
    protected ImpValue leave(NotNode node, InterpretExprVisitor visitor, ImpValue expressionValue) {
      return BooleanValue.create(!((BooleanValue) expressionValue).getValue());
    }

    @Override
    protected ImpValue leave(
        BinaryExpressionNode node, InterpretExprVisitor visitor, ImpValue lhs, ImpValue rhs) {
      return node.getOperator().evaluate(lhs, rhs);
    }

    @Override
    protected ImpValue leave(
        DowngradeNode node, InterpretExprVisitor visitor, ImpValue expressionValue) {
      return expressionValue;
    }
  }
}
