package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.RedeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.UndeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

/**
 * An abstract visitor that automatically maintains a variable context, while correctly handling
 * scoping. The visitor handles variables declared multiple times, and variable accesses to
 * undeclared variables by raising informative exceptions.
 *
 * <p>Implementations of this class need to provide {@code extract(declaration)} methods for
 * extracting data from declaration nodes. This data is associated with the declared variable in the
 * context. Additionally, a cloning method {@link #newScope()} is needed to handle new scopes.
 */
public abstract class ContextVisitor<
        SelfT extends
            ContextVisitor<
                    SelfT,
                    ContextValueT,
                    ReferenceResultT,
                    ExprResultT,
                    StmtResultT,
                    ProgramResultT>,
        ContextValueT,
        ReferenceResultT,
        ExprResultT,
        StmtResultT,
        ProgramResultT>
    extends AbstractProgramVisitor<
        SelfT, ReferenceResultT, ExprResultT, StmtResultT, ProgramResultT> {

  private Map<Variable, ContextValueT> context;

  /** Construct a visitor with empty context. */
  protected ContextVisitor() {
    context = HashMap.empty();
  }

  /**
   * Construct a new visitor where the context is copied from the given visitor.
   *
   * <p>Useful for implementing {@link #newScope()}.
   *
   * @param visitor instance to clone the context from
   */
  protected ContextVisitor(
      ContextVisitor<
              SelfT, ContextValueT, ReferenceResultT, ExprResultT, StmtResultT, ProgramResultT>
          visitor) {
    context = visitor.context;
  }

  /**
   * Return the value associated with the given variable.
   *
   * @param variable variable to lookup in the context
   * @throws UndeclaredVariableException if the variable is not declared
   */
  protected final ContextValueT get(Variable variable) throws UndeclaredVariableException {
    return context.get(variable).getOrElseThrow(() -> new UndeclaredVariableException(variable));
  }

  /** Return the current context. */
  protected final Map<Variable, ContextValueT> getContext() {
    return context;
  }

  protected abstract ContextValueT extract(VariableDeclarationNode node);

  protected abstract ContextValueT extract(ArrayDeclarationNode node);

  protected abstract ContextValueT extract(LetBindingNode node);

  /**
   * Return a clone of this visitor. This function is called when entering a new scope. Cloning the
   * visitor ensures exiting scope restores the previous context.
   *
   * <p>{@link #ContextVisitor(ContextVisitor)} should be used for cloning the base class.
   */
  protected abstract SelfT newScope();

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
    final StmtResultT result = super.visit(node);
    put(node.getVariable(), extract(node));
    return result;
  }

  @Override
  public final StmtResultT visit(LetBindingNode node) {
    final StmtResultT result = super.visit(node);
    put(node.getVariable(), extract(node));
    return result;
  }

  /** Add a mapping from a variable to a value. */
  private void put(Variable variable, ContextValueT value) {
    if (context.containsKey(variable)) {
      throw new RedeclaredVariableException(variable);
    }
    context = context.put(variable, value);
  }
}
