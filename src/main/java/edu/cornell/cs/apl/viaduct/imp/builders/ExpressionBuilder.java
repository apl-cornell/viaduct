package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.And;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.EqualTo;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.LessThan;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.LessThanOrEqualTo;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.Or;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.Plus;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerValue;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.security.Label;

/** Builds expressions. */
public class ExpressionBuilder {
  public ExpressionBuilder() {}

  public ExpressionNode var(String name) {
    return var(new Variable(name));
  }

  public ExpressionNode var(Variable name) {
    return new ReadNode(name);
  }

  public ExpressionNode lit(ImpValue value) {
    return new LiteralNode(value);
  }

  public ExpressionNode boolLit(boolean value) {
    return lit(new BooleanValue(value));
  }

  public ExpressionNode intLit(int value) {
    return lit(new IntegerValue(value));
  }

  public ExpressionNode not(ExpressionNode expression) {
    return new NotNode(expression);
  }

  public ExpressionNode or(ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.create(lhs, Or.create(), rhs);
  }

  public ExpressionNode and(ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.create(lhs, And.create(), rhs);
  }

  public ExpressionNode equals(ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.create(lhs, EqualTo.create(), rhs);
  }

  public ExpressionNode lt(ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.create(lhs, LessThan.create(), rhs);
  }

  public ExpressionNode leq(ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.create(lhs, LessThanOrEqualTo.create(), rhs);
  }

  public ExpressionNode plus(ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.create(lhs, Plus.create(), rhs);
  }

  public ExpressionNode downgrade(ExpressionNode expression, Label label) {
    return new DowngradeNode(expression, label);
  }

  public ExpressionNode declassify(ExpressionNode expression, Label label) {
    // TODO: assert integrity does not change
    return this.downgrade(expression, label);
  }

  public ExpressionNode endorse(ExpressionNode expression, Label label) {
    // TODO: assert confidentiality does not change
    return this.downgrade(expression, label);
  }
}
