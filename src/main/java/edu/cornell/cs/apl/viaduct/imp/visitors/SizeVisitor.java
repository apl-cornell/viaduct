package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
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

/** gives the size of an AST node. */
public class SizeVisitor implements AstVisitor<Integer> {
  private Integer visitBinary(BinaryExpressionNode binNode) {
    return 1 + binNode.getLhs().accept(this) + binNode.getRhs().accept(this);
  }

  public Integer visit(ReadNode readNode) {
    return 1;
  }

  public Integer visit(IntegerLiteralNode integerLiteralNode) {
    return 1;
  }

  public Integer visit(PlusNode plusNode) {
    return visitBinary(plusNode);
  }

  public Integer visit(BooleanLiteralNode booleanLiteralNode) {
    return 1;
  }

  public Integer visit(OrNode orNode) {
    return visitBinary(orNode);
  }

  public Integer visit(AndNode andNode) {
    return visitBinary(andNode);
  }

  public Integer visit(LessThanNode lessThanNode) {
    return visitBinary(lessThanNode);
  }

  public Integer visit(EqualNode equalNode) {
    return visitBinary(equalNode);
  }

  public Integer visit(LeqNode leqNode) {
    return visitBinary(leqNode);
  }

  public Integer visit(NotNode notNode) {
    return 1 + notNode.getExpression().accept(this);
  }

  public Integer visit(DowngradeNode downgradeNode) {
    Integer exprSize = downgradeNode.getExpression().accept(this);
    return 1 + exprSize;
  }

  public Integer visit(SkipNode skipNode) {
    return 0;
  }

  public Integer visit(VarDeclNode varDecl) {
    return 1;
  }

  public Integer visit(AssignNode assignNode) {
    return 1 + assignNode.getRhs().accept(this);
  }

  /** give size to if node. */
  public Integer visit(IfNode ifNode) {
    Integer guardSize = ifNode.getGuard().accept(this);
    Integer thenSize = ifNode.getThenBranch().accept(this);
    Integer elseSize = ifNode.getElseBranch().accept(this);
    return 1 + guardSize + thenSize + elseSize;
  }

  /** give size to block node. */
  public Integer visit(BlockNode blockNode) {
    Integer size = 0;
    for (StmtNode stmt : blockNode.getStatements()) {
      size += stmt.accept(this);
    }

    return size;
  }

  /** give size to send. */
  public Integer visit(SendNode sendNode) {
    return sendNode.getSentExpr().accept(this) + 1;
  }

  /** give size to recv. */
  public Integer visit(RecvNode recvNode) {
    return 1;
  }

  /** give size to annotation (none). */
  public Integer visit(AnnotationNode annotNode) {
    return 0;
  }
}
