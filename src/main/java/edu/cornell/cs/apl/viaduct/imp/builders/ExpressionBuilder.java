package edu.cornell.cs.apl.viaduct.imp.builders;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.security.Label;

/** Builds expressions. */
public class ExpressionBuilder {
  public ExpressionBuilder() {}

  public ExpressionNode var(String name) {
    return new ReadNode(new Variable(name));
  }

  public ExpressionNode var(Variable name) {
    return new ReadNode(name);
  }

  public ExpressionNode intLit(int value) {
    return new IntegerLiteralNode(value);
  }

  public ExpressionNode plus(ExpressionNode lhs, ExpressionNode rhs) {
    return new PlusNode(lhs, rhs);
  }

  public ExpressionNode boolLit(boolean value) {
    return new BooleanLiteralNode(value);
  }

  public ExpressionNode or(ExpressionNode lhs, ExpressionNode rhs) {
    return new OrNode(lhs, rhs);
  }

  public ExpressionNode and(ExpressionNode lhs, ExpressionNode rhs) {
    return new AndNode(lhs, rhs);
  }

  public ExpressionNode leq(ExpressionNode lhs, ExpressionNode rhs) {
    return new LeqNode(lhs, rhs);
  }

  public ExpressionNode lt(ExpressionNode lhs, ExpressionNode rhs) {
    return new LessThanNode(lhs, rhs);
  }

  public ExpressionNode equals(ExpressionNode lhs, ExpressionNode rhs) {
    return new EqualNode(lhs, rhs);
  }

  public ExpressionNode not(ExpressionNode expression) {
    return new NotNode(expression);
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
