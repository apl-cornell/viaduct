package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
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
import java.util.ArrayList;
import java.util.List;

/** returns a clone of the AST. */
public class CloneVisitor implements ExprVisitor<ExpressionNode>, StmtVisitor<StmtNode> {
  @Override
  public ExpressionNode visit(ReadNode readNode) {
    return new ReadNode(readNode.getVariable());
  }

  @Override
  public ExpressionNode visit(IntegerLiteralNode integerLiteralNode) {
    return new IntegerLiteralNode(integerLiteralNode.getValue());
  }

  @Override
  public ExpressionNode visit(PlusNode plusNode) {
    ExpressionNode newLhs = plusNode.getLhs().accept(this);
    ExpressionNode newRhs = plusNode.getRhs().accept(this);
    return new PlusNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(BooleanLiteralNode booleanLiteralNode) {
    return new BooleanLiteralNode(booleanLiteralNode.getValue());
  }

  @Override
  public ExpressionNode visit(OrNode orNode) {
    ExpressionNode newLhs = orNode.getLhs().accept(this);
    ExpressionNode newRhs = orNode.getRhs().accept(this);
    return new OrNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(AndNode andNode) {
    ExpressionNode newLhs = andNode.getLhs().accept(this);
    ExpressionNode newRhs = andNode.getRhs().accept(this);
    return new AndNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(LessThanNode lessThanNode) {
    ExpressionNode newLhs = lessThanNode.getLhs().accept(this);
    ExpressionNode newRhs = lessThanNode.getRhs().accept(this);
    return new LessThanNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(EqualNode equalNode) {
    ExpressionNode newLhs = equalNode.getLhs().accept(this);
    ExpressionNode newRhs = equalNode.getRhs().accept(this);
    return new EqualNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(LeqNode leqNode) {
    ExpressionNode newLhs = leqNode.getLhs().accept(this);
    ExpressionNode newRhs = leqNode.getRhs().accept(this);
    return new LeqNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    ExpressionNode newExpr = notNode.getExpression().accept(this);
    return new NotNode(newExpr);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    ExpressionNode newExpr = downgradeNode.getExpression().accept(this);
    return new DowngradeNode(newExpr, downgradeNode.getLabel());
  }

  @Override
  public StmtNode visit(SkipNode skipNode) {
    return new SkipNode();
  }

  @Override
  public StmtNode visit(VarDeclNode varDecl) {
    return new VarDeclNode(varDecl.getVariable(), varDecl.getLabel());
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return new AssignNode(assignNode.getVariable(), newRhs);
  }

  /** give traverse children and do nothing. */
  @Override
  public StmtNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    StmtNode newThen = ifNode.getThenBranch().accept(this);
    StmtNode newElse = ifNode.getElseBranch().accept(this);
    return new IfNode(newGuard, newThen, newElse);
  }

  /** traverse children and do nothing. */
  @Override
  public StmtNode visit(BlockNode blockNode) {
    List<StmtNode> newList = new ArrayList<>();
    for (StmtNode stmt : blockNode) {
      newList.add(stmt.accept(this));
    }
    return new BlockNode(newList);
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    ExpressionNode newExpr = sendNode.getSentExpr().accept(this);
    return new SendNode(sendNode.getRecipient(), newExpr);
  }

  @Override
  public StmtNode visit(RecvNode recvNode) {
    return new RecvNode(recvNode.getSender(), recvNode.getVar());
  }

  @Override
  public StmtNode visit(AnnotationNode annotNode) {
    return new AnnotationNode(annotNode.getAnnotationString(), annotNode.getAnnotation());
  }
}
