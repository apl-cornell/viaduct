package edu.cornell.cs.apl.viaduct;

/** builds expressions. */
public class ExprBuilder {
  public ExprBuilder() {}

  public ExprNode var(String varName) {
    return new VarLookupNode(new Variable(varName));
  }

  public ExprNode intLit(int i) {
    return new IntLiteralNode(i);
  }

  public ExprNode plus(ExprNode lhs, ExprNode rhs) {
    return new PlusNode(lhs, rhs);
  }

  public ExprNode boolLit(boolean b) {
    return new BoolLiteralNode(b);
  }

  public ExprNode or(ExprNode lhs, ExprNode rhs) {
    return new OrNode(lhs, rhs);
  }

  public ExprNode and(ExprNode lhs, ExprNode rhs) {
    return new AndNode(lhs, rhs);
  }

  public ExprNode leq(ExprNode lhs, ExprNode rhs) {
    return new LeqNode(lhs, rhs);
  }

  public ExprNode lt(ExprNode lhs, ExprNode rhs) {
    return new LessThanNode(lhs, rhs);
  }

  public ExprNode equals(ExprNode lhs, ExprNode rhs) {
    return new EqualNode(lhs, rhs);
  }

  public ExprNode not(ExprNode negatedExpr) {
    return new NotNode(negatedExpr);
  }

  public ExprNode declassify(ExprNode declExpr, Label label) {
    return new DeclassifyNode(declExpr, label);
  }

  public ExprNode endorse(ExprNode endoExpr, Label label) {
    return new EndorseNode(endoExpr, label);
  }
}
