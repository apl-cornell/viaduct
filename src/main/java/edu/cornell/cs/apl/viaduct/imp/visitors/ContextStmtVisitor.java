package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.errors.NameClashError;
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.HasLocation;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

/**
 * An abstract visitor that maintains a variable context with scoping. The visitor handles variables
 * declared multiple times, and variable accesses to undeclared variables by raising informative
 * exceptions.
 *
 * <p>Implementations of this class need to provide {@code extract(declaration)} methods for
 * extracting data from declaration nodes. This data is associated with the declared variable in the
 * context. {@code extract} methods are guaranteed to be called precisely once per declaration node,
 * right after the {@code leave} method for that node.
 *
 * <p>Additionally, a cloning method {@link #newScope()} is needed to handle new scopes.
 */
public abstract class ContextStmtVisitor<
        SelfT extends
            ContextStmtVisitor<SelfT, ContextValueT, ReferenceResultT, ExprResultT, StmtResultT>,
        ContextValueT,
        ReferenceResultT,
        ExprResultT,
        StmtResultT>
    extends AbstractStmtVisitor<SelfT, ReferenceResultT, ExprResultT, StmtResultT> {

  private Map<Variable, ContextValueT> context;

  /** Construct a visitor with empty context. */
  protected ContextStmtVisitor() {
    context = HashMap.empty();
  }

  /**
   * Construct a new visitor where the context is copied from the given visitor.
   *
   * <p>Useful for implementing {@link #newScope()}.
   *
   * @param visitor instance to clone the context from
   */
  protected ContextStmtVisitor(
      ContextStmtVisitor<SelfT, ContextValueT, ReferenceResultT, ExprResultT, StmtResultT>
          visitor) {
    context = visitor.context;
  }

  /**
   * Return the value associated with the given variable.
   *
   * @param variable variable to lookup in the context
   * @throws UndefinedNameError if the variable is not declared
   */
  protected final ContextValueT get(Variable variable) throws UndefinedNameError {
    return context.get(variable).getOrElseThrow(() -> new UndefinedNameError(variable));
  }

  /** Return the current context. */
  protected final Map<Variable, ContextValueT> getContext() {
    return context;
  }

  protected abstract ContextValueT extract(VariableDeclarationNode node);

  protected abstract ContextValueT extract(ArrayDeclarationNode node, ExprResultT length);

  protected abstract ContextValueT extract(LetBindingNode node, ExprResultT rhs);

  /**
   * Return a clone of this visitor. This function is called when entering a new scope. Cloning the
   * visitor ensures exiting scope restores the previous context.
   *
   * <p>Note that if the visitors returned by {@link #getReferenceVisitor()} and {@link
   * #getExpressionVisitor()} need context information and therefore maintain references a context
   * visitor, they should also be cloned and the clones should reference the newly created visitor.
   *
   * <p>{@link #ContextStmtVisitor(ContextStmtVisitor)} should be used for cloning the base class.
   */
  protected abstract SelfT newScope();

  @Override
  protected final SelfT enter(ForNode node) {
    return newScope();
  }

  @Override
  protected final SelfT enter(BlockNode node) {
    return newScope();
  }

  @Override
  public final StmtResultT visit(VariableDeclarationNode node) {
    final StmtResultT result = super.visit(node);
    put(node.getVariable(), extract(node));
    return result;
  }

  @Override
  public final StmtResultT visit(ArrayDeclarationNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT length = node.getLength().accept(visitor.getExpressionVisitor());
    final StmtResultT result = leave(node, visitor, length);
    put(node.getVariable(), extract(node, length));
    return result;
  }

  @Override
  public final StmtResultT visit(LetBindingNode node) {
    final SelfT visitor = enter(node);
    final ExprResultT rhs = node.getRhs().accept(visitor.getExpressionVisitor());
    final StmtResultT result = leave(node, visitor, rhs);
    put(node.getVariable(), extract(node, rhs));
    return result;
  }

  /** Add a mapping from a variable to a value. */
  private void put(Variable variable, ContextValueT value) {
    if (context.containsKey(variable)) {
      final HasLocation previousDeclaration = context.keySet().find(variable::equals).getOrNull();
      throw new NameClashError(previousDeclaration, variable);
    }
    context = context.put(variable, value);
  }
}
