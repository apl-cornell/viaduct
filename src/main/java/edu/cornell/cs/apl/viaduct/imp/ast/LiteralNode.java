package edu.cornell.cs.apl.viaduct.imp.ast;

/**
 * Superclass of literal constants.
 *
 * <p>Literals of specific types (like integer or boolean) should inherit from this class.
 */
public abstract class LiteralNode<V> extends ExpressionNode {
  private final V value;

  public LiteralNode(V value) {
    this.value = value;
  }

  public V getValue() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof BooleanLiteralNode) {
      LiteralNode otherLit = (LiteralNode) other;
      return otherLit.value.equals(this.value);

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }
}
