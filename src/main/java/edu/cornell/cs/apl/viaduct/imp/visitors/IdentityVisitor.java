package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.DuplicateProcessDefinitionException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
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
    implements ReferenceVisitor<Reference>,
        ExprVisitor<ExpressionNode>,
        StmtVisitor<StmtNode>,
        ProgramVisitor<ProgramNode> {

  public Reference run(Reference reference) {
    return reference.accept(this);
  }

  public ExpressionNode run(ExpressionNode expression) {
    return expression.accept(this);
  }

  public StmtNode run(StmtNode statement) {
    return statement.accept(this);
  }

  public ProgramNode run(ProgramNode program) {
    return program.accept(this);
  }

  @Override
  public Reference visit(Variable variable) {
    return variable;
  }

  @Override
  public Reference visit(ArrayIndex arrayIndex) {
    ExpressionNode newIndex = arrayIndex.getIndex().accept(this);
    return ArrayIndex.create(arrayIndex.getArray(), newIndex);
  }

  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    return literalNode;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    Reference newReference = readNode.getReference().accept(this);
    return ReadNode.create(newReference);
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    ExpressionNode newExpr = notNode.getExpression().accept(this);
    return NotNode.create(newExpr);
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
    return DowngradeNode.create(newExpr, downgradeNode.getLabel());
  }

  @Override
  public StmtNode visit(VariableDeclarationNode declNode) {
    return VariableDeclarationNode.create(
        declNode.getVariable(), declNode.getType(), declNode.getLabel());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    ExpressionNode newLength = arrayDeclNode.getLength().accept(this);
    return ArrayDeclarationNode.create(
        arrayDeclNode.getVariable(),
        newLength,
        arrayDeclNode.getType(),
        arrayDeclNode.getLabel());
  }

  @Override
  public StmtNode visit(LetBindingNode letBindingNode) {
    return LetBindingNode.create(
        letBindingNode.getVariable(), letBindingNode.getRhs().accept(this));
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    Reference newLhs = assignNode.getLhs().accept(this);
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return AssignNode.create(newLhs, newRhs);
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    StmtNode newThen = ifNode.getThenBranch().accept(this);
    StmtNode newElse = ifNode.getElseBranch().accept(this);
    return IfNode.create(newGuard, newThen, newElse);
  }

  @Override
  public StmtNode visit(WhileNode whileNode) {
    ExpressionNode newGuard = whileNode.getGuard().accept(this);
    StmtNode newBody = whileNode.getBody().accept(this);
    return WhileNode.create(newGuard, newBody);
  }

  @Override
  public StmtNode visit(ForNode forNode) {
    StmtNode newInit = forNode.getInitialize().accept(this);
    ExpressionNode newGuard = forNode.getGuard().accept(this);
    StmtNode newUpdate = forNode.getUpdate().accept(this);
    StmtNode newBody = forNode.getBody().accept(this);
    return ForNode.create(newInit, newGuard, newUpdate, newBody);
  }

  @Override
  public StmtNode visit(LoopNode loopNode) {
    StmtNode newBody = loopNode.getBody().accept(this);
    return LoopNode.create(newBody);
  }

  @Override
  public StmtNode visit(BreakNode breakNode) {
    ExpressionNode newLevel = breakNode.getLevel().accept(this);
    return BreakNode.create(newLevel);
  }

  @Override
  public StmtNode visit(BlockNode blockNode) {
    List<StmtNode> newList = new LinkedList<>();
    for (StmtNode stmt : blockNode) {
      newList.add(stmt.accept(this));
    }
    return BlockNode.create(newList);
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    ExpressionNode newExpr = sendNode.getSentExpression().accept(this);
    return SendNode.create(sendNode.getRecipient(), newExpr);
  }

  @Override
  public StmtNode visit(ReceiveNode receiveNode) {
    return ReceiveNode.create(
        receiveNode.getVariable(), receiveNode.getRecvType(), receiveNode.getSender());
  }

  @Override
  public ProgramNode visit(ProgramNode programNode) {
    final ProgramNode.Builder builder = ProgramNode.builder();
    try {
      for (Tuple2<ProcessName, StmtNode> process : programNode) {
        builder.addProcess(process._1, run(process._2));
      }
    } catch (DuplicateProcessDefinitionException e) {
      // This is impossible
      throw new RuntimeException(e);
    }
    builder.addHosts(programNode.getHostTrustConfiguration());
    return builder.build();
  }

  @Override
  public StmtNode visit(AssertNode assertNode) {
    ExpressionNode newExpr = assertNode.getExpression().accept(this);
    return AssertNode.create(newExpr);
  }
}
