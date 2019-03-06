package edu.cornell.cs.apl.viaduct.imp.ast;

/**
 * Superclass of literal constants.
 *
 * <p>Literals of specific types (like integer or boolean) should inherit from this class.
 */
public abstract class LiteralNode<V> implements ExpressionNode {
  private final V value;

  public LiteralNode(V value) {
    this.value = value;
  }

  public V getValue() {
    return value;
  }
}
