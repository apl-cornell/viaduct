package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.surface.AndNode;
import edu.cornell.cs.apl.viaduct.surface.AssignNode;
import edu.cornell.cs.apl.viaduct.surface.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.surface.BlockNode;
import edu.cornell.cs.apl.viaduct.surface.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.surface.DowngradeNode;
import edu.cornell.cs.apl.viaduct.surface.EqualNode;
import edu.cornell.cs.apl.viaduct.surface.IfNode;
import edu.cornell.cs.apl.viaduct.surface.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.surface.LeqNode;
import edu.cornell.cs.apl.viaduct.surface.LessThanNode;
import edu.cornell.cs.apl.viaduct.surface.NotNode;
import edu.cornell.cs.apl.viaduct.surface.OrNode;
import edu.cornell.cs.apl.viaduct.surface.PlusNode;
import edu.cornell.cs.apl.viaduct.surface.ReadNode;
import edu.cornell.cs.apl.viaduct.surface.SkipNode;
import edu.cornell.cs.apl.viaduct.surface.StmtNode;
import edu.cornell.cs.apl.viaduct.surface.VarDeclNode;

/** pretty-prints an AST. */
public class PrintVisitor implements ExprVisitor<String>, StmtVisitor<String> {
  private static final int INDENT_LEVEL = 4;
  private int indent;

  public PrintVisitor() {
    this.indent = 0;
  }

  private String getIndent() {
    StringBuilder indentStr = new StringBuilder();
    for (int i = 0; i < this.indent; i++) {
      indentStr.append(" ");
    }

    return indentStr.toString();
  }

  private String visitBinary(BinaryExpressionNode binNode, String op) {
    String lhsStr = binNode.getLhs().accept(this);
    String rhsStr = binNode.getRhs().accept(this);
    return lhsStr + " " + op + " " + rhsStr;
  }

  // TODO: remove worthless comments.

  /** print node. */
  public String visit(ReadNode readNode) {
    return readNode.getVariable().getName();
  }

  /** print node. */
  public String visit(IntegerLiteralNode integerLiteralNode) {
    return Integer.toString(integerLiteralNode.getValue());
  }

  /** print node. */
  public String visit(PlusNode plusNode) {
    return visitBinary(plusNode, "+");
  }

  /** print node. */
  public String visit(BooleanLiteralNode booleanLiteralNode) {
    return Boolean.toString(booleanLiteralNode.getValue());
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
  public String visit(LessThanNode lessThanNode) {
    return visitBinary(lessThanNode, "<");
  }

  /** print node. */
  public String visit(EqualNode equalNode) {
    return visitBinary(equalNode, "==");
  }

  /** print node. */
  public String visit(LeqNode leqNode) {
    return visitBinary(leqNode, "<=");
  }

  /** print node. */
  public String visit(NotNode notNode) {
    return "!" + notNode.getExpression().accept(this);
  }

  /** print node. */
  public String visit(DowngradeNode downgradeNode) {
    // TODO: special case declassfy and endorse
    String expressionStr = downgradeNode.getExpression().accept(this);
    String labelStr = downgradeNode.getExpression().toString();
    return "downgrade(" + expressionStr + ", " + labelStr + ")";
  }

  /** print node. */
  public String visit(SkipNode skipNode) {
    return getIndent() + "skip";
  }

  /** print node. */
  public String visit(VarDeclNode varDecl) {
    String varStr = varDecl.getVariable().getName();
    String labelStr = varDecl.getLabel().toString();
    return getIndent() + varStr + " : " + labelStr;
  }

  /** print node. */
  public String visit(AssignNode assignNode) {
    String varStr = assignNode.getVariable().getName();
    String rhsStr = assignNode.getRhs().accept(this);
    return getIndent() + varStr + " <- " + rhsStr;
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
  public String visit(BlockNode blockNode) {
    StringBuilder buf = new StringBuilder();
    for (StmtNode stmt : blockNode.getStatements()) {
      buf.append(stmt.accept(this));
      buf.append('\n');
    }

    return buf.toString();
  }
}
