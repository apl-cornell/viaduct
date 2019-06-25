package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.DuplicateProcessDefinitionException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayAccessNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import io.vavr.Tuple2;
import java.util.LinkedList;
import java.util.List;

/**
 * A visitor that traverses the AST and returns it unchanged.
 *
 * <p>This class provides a default "change nothing" behavior, and is meant as a template for other
 * visitors. Visitors that only change a small subset of AST nodes should inherit from this class
 * and override only the cases that do something interesting.
 */
public abstract class IdentityVisitor
    implements ExprVisitor<ExpressionNode>,
        StmtVisitor<StmtNode>,
        LExprVisitor<LExpressionNode>,
        ProgramVisitor<ProgramNode> {

  public ExpressionNode run(ExpressionNode expr) {
    return expr.accept(this);
  }

  public StmtNode run(StmtNode stmt) {
    return stmt.accept(this);
  }

  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    return literalNode;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    return readNode;
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    ExpressionNode newExpr = notNode.getExpression().accept(this);
    return new NotNode(newExpr);
  }

  @Override
  public ExpressionNode visit(BinaryExpressionNode binaryExpressionNode) {
    ExpressionNode newLhs = binaryExpressionNode.getLhs().accept(this);
    ExpressionNode newRhs = binaryExpressionNode.getRhs().accept(this);
    return BinaryExpressionNode.create(newLhs, binaryExpressionNode.getOperator(), newRhs);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    ExpressionNode newExpr = downgradeNode.getExpression().accept(this);
    return new DowngradeNode(newExpr, downgradeNode.getLabel());
  }

  @Override
  public ExpressionNode visit(ArrayAccessNode arrAccessNode) {
    ExpressionNode newIndex = arrAccessNode.getIndex().accept(this);
    return new ArrayAccessNode(arrAccessNode.getVariable(), newIndex);
  }

  @Override
  public LExpressionNode visit(ArrayIndexNode arrIndexNode) {
    ExpressionNode newIndex = arrIndexNode.getIndex().accept(this);
    return new ArrayIndexNode(arrIndexNode.getVariable(), newIndex);
  }

  @Override
  public LExpressionNode visit(LReadNode lreadNode) {
    return lreadNode;
  }

  @Override
  public StmtNode visit(DeclarationNode declNode) {
    return new DeclarationNode(declNode.getVariable(), declNode.getType(), declNode.getLabel());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    return new ArrayDeclarationNode(
        arrayDeclNode.getVariable(),
        arrayDeclNode.getLength(),
        arrayDeclNode.getType(),
        arrayDeclNode.getLabel());
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    LExpressionNode newLhs = assignNode.getLhs().accept(this);
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return new AssignNode(newLhs, newRhs);
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    StmtNode newThen = ifNode.getThenBranch().accept(this);
    StmtNode newElse = ifNode.getElseBranch().accept(this);
    return new IfNode(newGuard, newThen, newElse);
  }

  @Override
  public StmtNode visit(WhileNode whileNode) {
    ExpressionNode newGuard = whileNode.getGuard().accept(this);
    StmtNode newBody = whileNode.getBody().accept(this);
    return new WhileNode(newGuard, newBody);
  }

  @Override
  public StmtNode visit(ForNode forNode) {
    StmtNode newInit = forNode.getInitialize().accept(this);
    ExpressionNode newGuard = forNode.getGuard().accept(this);
    StmtNode newUpdate = forNode.getUpdate().accept(this);
    StmtNode newBody = forNode.getBody().accept(this);
    return new ForNode(newInit, newGuard, newUpdate, newBody);
  }

  @Override
  public StmtNode visit(BlockNode blockNode) {
    List<StmtNode> newList = new LinkedList<>();
    for (StmtNode stmt : blockNode) {
      newList.add(stmt.accept(this));
    }
    return new BlockNode(newList);
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    ExpressionNode newExpr = sendNode.getSentExpression().accept(this);
    return new SendNode(sendNode.getRecipient(), newExpr);
  }

  @Override
  public StmtNode visit(ReceiveNode receiveNode) {
    return new ReceiveNode(receiveNode.getVariable(), receiveNode.getSender());
  }

  @Override
  public ProgramNode visit(ProgramNode programNode) {
    final ProgramNode.Builder builder = ProgramNode.builder();
    try {
      for (Tuple2<ProcessName, StmtNode> process : programNode) {
        builder.addProcess(process._1, process._2.accept(this));
      }
    } catch (DuplicateProcessDefinitionException e) {
      // This is impossible
      throw new Error(e);
    }
    builder.addHosts(programNode.getHostTrustConfiguration());
    return builder.build();
  }

  @Override
  public StmtNode visit(AssertNode assertNode) {
    ExpressionNode newExpr = assertNode.getExpression().accept(this);
    return new AssertNode(newExpr);
  }
}
