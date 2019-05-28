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

/** do nothing. can be subclassed to do something for specific AST nodes. */
public class VoidVisitor implements AstVisitor<Void> {
  private Void visitBinary(BinaryExpressionNode binNode) {
    binNode.getLhs().accept(this);
    binNode.getRhs().accept(this);
    return null;
  }

  @Override
  public Void visit(ReadNode readNode) {
    return null;
  }

  @Override
  public Void visit(IntegerLiteralNode integerLiteralNode) {
    return null;
  }

  @Override
  public Void visit(PlusNode plusNode) {
    return visitBinary(plusNode);
  }

  @Override
  public Void visit(BooleanLiteralNode booleanLiteralNode) {
    return null;
  }

  @Override
  public Void visit(OrNode orNode) {
    return visitBinary(orNode);
  }

  @Override
  public Void visit(AndNode andNode) {
    return null;
  }

  @Override
  public Void visit(LessThanNode lessThanNode) {
    return null;
  }

  @Override
  public Void visit(EqualNode equalNode) {
    return null;
  }

  @Override
  public Void visit(LeqNode leqNode) {
    return null;
  }

  @Override
  public Void visit(NotNode notNode) {
    notNode.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(DowngradeNode downgradeNode) {
    downgradeNode.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(SkipNode skipNode) {
    return null;
  }

  @Override
  public Void visit(VarDeclNode varDecl) {
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    assignNode.getRhs().accept(this);
    return null;
  }

  /** give traverse children and do nothing. */
  @Override
  public Void visit(IfNode ifNode) {
    ifNode.getGuard().accept(this);
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);
    return null;
  }

  /** traverse children and do nothing. */
  @Override
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    sendNode.getSentExpr().accept(this);
    return null;
  }

  @Override
  public Void visit(RecvNode recvNode) {
    return null;
  }

  @Override
  public Void visit(AnnotationNode annotNode) {
    return null;
  }
}
