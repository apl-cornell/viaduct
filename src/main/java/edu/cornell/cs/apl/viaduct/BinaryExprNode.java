package edu.cornell.cs.apl.viaduct;

/**  binary operation expression. */
public interface BinaryExprNode extends ExprNode {
  ExprNode getLhs();

  ExprNode getRhs();
}
