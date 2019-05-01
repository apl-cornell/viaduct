package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;

/** Pretty-prints an AST. */
public class PrintVisitor implements AstVisitor<String> {
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

  /** print send. */
  public String visit(SendNode sendNode) {
    String exprStr = sendNode.getSentExpr().accept(this);
    String recipStr = sendNode.getRecipient().toString();
    return String.format("send %s to %s", exprStr, recipStr);
  }

  /** print recv. */
  public String visit(RecvNode recvNode) {
    String senderStr = recvNode.getSender();
    String varStr = recvNode.getVar().toString();
    return String.format("%s <- recv from %s", varStr, senderStr);
  }
}
