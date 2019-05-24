package edu.cornell.cs.apl.viaduct.imp.ast;

import java.util.Objects;

/**
 * Superclass of literal constants.
 *
 * <p>Literals of specific types (like integer or boolean) should inherit from this class.
 */
public abstract class LiteralNode<V> extends ExpressionNode {
  protected final V value;

  LiteralNode(V value) {
    this.value = value;
  }

  public V getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }
}
