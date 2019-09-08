package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperator;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.And;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.EqualTo;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.LessThan;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.LessThanOrEqualTo;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.Or;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryOperators.Plus;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;

/** Builds expressions. */
public final class ExpressionBuilder {
  public ExpressionBuilder() {}

  public ExpressionNode var(String name) {
    return var(Variable.create(name));
  }

  public ExpressionNode var(Binding<?> binding) {
    return var(Variable.create(binding));
  }

  public ExpressionNode var(Variable variable) {
    return ReadNode.builder().setReference(variable).build();
  }

  public ExpressionNode lit(ImpValue value) {
    return LiteralNode.builder().setValue(value).build();
  }

  public ExpressionNode boolLit(boolean value) {
    return lit(BooleanValue.create(value));
  }

  public ExpressionNode intLit(int value) {
    return lit(IntegerValue.create(value));
  }

  public ExpressionNode read(ReferenceNode ref) {
    return ReadNode.builder().setReference(ref).build();
  }

  public ExpressionNode not(ExpressionNode expression) {
    return NotNode.builder().setExpression(expression).build();
  }

  private ExpressionNode binop(BinaryOperator op, ExpressionNode lhs, ExpressionNode rhs) {
    return BinaryExpressionNode.builder().setOperator(op).setLhs(lhs).setRhs(rhs).build();
  }

  public ExpressionNode or(ExpressionNode lhs, ExpressionNode rhs) {
    return binop(Or.create(), lhs, rhs);
  }

  public ExpressionNode and(ExpressionNode lhs, ExpressionNode rhs) {
    return binop(And.create(), lhs, rhs);
  }

  public ExpressionNode equals(ExpressionNode lhs, ExpressionNode rhs) {
    return binop(EqualTo.create(), lhs, rhs);
  }

  public ExpressionNode lt(ExpressionNode lhs, ExpressionNode rhs) {
    return binop(LessThan.create(), lhs, rhs);
  }

  public ExpressionNode leq(ExpressionNode lhs, ExpressionNode rhs) {
    return binop(LessThanOrEqualTo.create(), lhs, rhs);
  }

  public ExpressionNode plus(ExpressionNode lhs, ExpressionNode rhs) {
    return binop(Plus.create(), lhs, rhs);
  }
}
