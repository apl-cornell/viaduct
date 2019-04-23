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
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;

/** gives the size of an AST node. */
public class SizeVisitor implements AstVisitor<Integer> {
  private Integer visitBinary(BinaryExpressionNode binNode) {
    return 1 + binNode.getLhs().accept(this) + binNode.getRhs().accept(this);
  }

  /** give node size. */
  public Integer visit(ReadNode readNode) {
    return 1;
  }

  /** give node size. */
  public Integer visit(IntegerLiteralNode integerLiteralNode) {
    return 1;
  }

  /** give node size. */
  public Integer visit(PlusNode plusNode) {
    return visitBinary(plusNode);
  }

  /** give node size. */
  public Integer visit(BooleanLiteralNode booleanLiteralNode) {
    return 1;
  }

  /** give node size. */
  public Integer visit(OrNode orNode) {
    return visitBinary(orNode);
  }

  /** give node size. */
  public Integer visit(AndNode andNode) {
    return visitBinary(andNode);
  }

  /** give node size. */
  public Integer visit(LessThanNode lessThanNode) {
    return visitBinary(lessThanNode);
  }

  /** give node size. */
  public Integer visit(EqualNode equalNode) {
    return visitBinary(equalNode);
  }

  /** give node size. */
  public Integer visit(LeqNode leqNode) {
    return visitBinary(leqNode);
  }

  /** give node size. */
  public Integer visit(NotNode notNode) {
    return 1 + notNode.getExpression().accept(this);
  }

  /** give node size. */
  public Integer visit(DowngradeNode downgradeNode) {
    Integer exprSize = downgradeNode.getExpression().accept(this);
    return 1 + exprSize;
  }

  /** give node size. */
  public Integer visit(SkipNode skipNode) {
    return 0;
  }

  /** give node size. */
  public Integer visit(VarDeclNode varDecl) {
    return 1;
  }

  /** give node size. */
  public Integer visit(AssignNode assignNode) {
    return 1 + assignNode.getRhs().accept(this);
  }

  /** give node size. */
  public Integer visit(IfNode ifNode) {
    Integer guardSize = ifNode.getGuard().accept(this);
    Integer thenSize = ifNode.getThenBranch().accept(this);
    Integer elseSize = ifNode.getElseBranch().accept(this);
    return 1 + guardSize + thenSize + elseSize;
  }

  /** give node size. */
  public Integer visit(BlockNode blockNode) {
    Integer size = 0;
    for (StmtNode stmt : blockNode.getStatements()) {
      size += stmt.accept(this);
    }

    return size;
  }
}
