package edu.cornell.cs.apl.viaduct.imp.ast;

import com.google.auto.value.AutoValue;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;

@AutoValue
public abstract class ArrayDeclarationNode extends StmtNode {
  /**
   * Declare a statically allocated array with the given length.
   *
   * @param variable name of the array
   * @param length number of elements in the array
   * @param type type of the elements in the array
   * @param label security label of the array and all its elements
   */
  public static ArrayDeclarationNode create(
      Variable variable, ExpressionNode length, ImpType type, Label label) {
    return new AutoValue_ArrayDeclarationNode(variable, length, type, label);
  }

  public abstract Variable getVariable();

  public abstract ExpressionNode getLength();

  public abstract ImpType getType();

  public abstract Label getLabel();

  @Override
  public final <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }
}
