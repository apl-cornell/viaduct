package edu.cornell.cs.apl.viaduct;

/**  pretty-prints an AST. */
public class PrintVisitor implements ExprVisitor<String>, StmtVisitor<String> {
  static int INDENT_LEVEL = 2;
  int indent;

  public PrintVisitor() {
    this.indent = 0;
  }

  protected String getIndent() {
    StringBuffer indentStr = new StringBuffer();
    for (int i = 0; i < this.indent; i++) {
      indentStr.append(" ");
    }

    return indentStr.toString();
  }

  protected String visitBinary(BinaryExprNode binNode, String op) {
    String lhsStr = binNode.getLhs().accept(this);
    String rhsStr = binNode.getRhs().accept(this);
    return lhsStr + " " + op + " " + rhsStr;
  }

  /** print node. */
  public String visit(VarLookupNode var) {
    return var.getVar().getName();
  }

  /** print node. */
  public String visit(IntLiteralNode intLit) {
    return Integer.toString(intLit.getVal());
  }

  /** print node. */
  public String visit(PlusNode plusNode) {
    return visitBinary(plusNode, "+");
  }

  /** print node. */
  public String visit(BoolLiteralNode boolLit) {
    return Boolean.toString(boolLit.getVal());
  }

  /** print node. */
  public String visit(OrNode orNode) {
    return visitBinary(orNode, "||");
  }

  /** print node. */
  public String visit(AndNode andNode) {
    return visitBinary(andNode, "&&");
  }

  /** print node. */
  public String visit(LessThanNode ltNode) {
    return visitBinary(ltNode, "<");
  }

  /** print node. */
  public String visit(EqualNode eqNode) {
    return visitBinary(eqNode, "==");
  }

  /** print node. */
  public String visit(LeqNode leqNode) {
    return visitBinary(leqNode, "<=");
  }

  /** print node. */
  public String visit(NotNode notNode) {
    return "!" + notNode.getNegatedExpr().accept(this);
  }

  /** print node. */
  public String visit(DeclassifyNode declNode) {
    String declExprStr = declNode.getDeclassifiedExpr().accept(this);
    String labelStr = declNode.getDowngradeLabel().toString();
    return "declassify(" + declExprStr + ", " + labelStr + ")";
  }

  /** print node. */
  public String visit(EndorseNode endoNode) {
    String declExprStr = endoNode.getEndorsedExpr().accept(this);
    String labelStr = endoNode.getDowngradeLabel().toString();
    return "endorse(" + declExprStr + ", " + labelStr + ")";
  }

  /** print node. */
  public String visit(SkipNode skipNode) {
    String indentStr = getIndent();
    return indentStr + "skip";
  }

  /** print node. */
  public String visit(VarDeclNode varDecl) {
    String varStr = varDecl.getDeclaredVar().getName();
    String labelStr = varDecl.getVarLabel().toString();
    String indentStr = getIndent();
    return indentStr + varStr + " : " + labelStr;
  }

  /** print node. */
  public String visit(AssignNode assignNode) {
    String varStr = assignNode.getVar().getName();
    String rhsStr = assignNode.getRhs().accept(this);
    String indentStr = getIndent();
    return indentStr + varStr + " <- " + rhsStr;
  }

  /** print node. */
  public String visit(IfNode ifNode) {
    String guardStr = ifNode.getGuard().accept(this);
    this.indent += INDENT_LEVEL;
    String thenStr = ifNode.getThenBranch().accept(this);
    String elseStr = ifNode.getElseBranch().accept(this);
    this.indent -= INDENT_LEVEL;

    String indentStr = getIndent();
    return indentStr
        + "if ("
        + guardStr
        + ") {\n"
        + thenStr
        + "\n"
        + indentStr
        + "} else {"
        + "\n"
        + elseStr
        + "\n"
        + indentStr
        + "}";
  }

  /** print node. */
  public String visit(SeqNode seqNode) {
    StringBuffer buf = new StringBuffer();
    for (StmtNode stmt : seqNode.getStmts()) {
      buf.append(stmt.accept(this) + "\n");
    }

    return buf.toString();
  }
}
