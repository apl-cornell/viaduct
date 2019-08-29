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
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode.DowngradeType;
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
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.security.Label;
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
        StmtVisitor<StatementNode>,
        ProgramVisitor<ProgramNode> {

  public Reference run(Reference reference) {
    return reference.accept(this);
  }

  public ExpressionNode run(ExpressionNode expression) {
    return expression.accept(this);
  }

  public StatementNode run(StatementNode statement) {
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
    final ExpressionNode newExpr = downgradeNode.getExpression().accept(this);
    final Label fromLabel = downgradeNode.getFromLabel();
    final Label toLabel = downgradeNode.getToLabel();
    final DowngradeType downgradeType = downgradeNode.getDowngradeType();
    return DowngradeNode.create(newExpr, fromLabel, toLabel, downgradeType);
  }

  @Override
  public StatementNode visit(VariableDeclarationNode declNode) {
    return VariableDeclarationNode.create(
        declNode.getVariable(), declNode.getType(), declNode.getLabel());
  }

  @Override
  public StatementNode visit(ArrayDeclarationNode arrayDeclNode) {
    ExpressionNode newLength = arrayDeclNode.getLength().accept(this);
    return ArrayDeclarationNode.create(
        arrayDeclNode.getVariable(), newLength, arrayDeclNode.getType(), arrayDeclNode.getLabel());
  }

  @Override
  public StatementNode visit(LetBindingNode letBindingNode) {
    return LetBindingNode.create(
        letBindingNode.getVariable(), letBindingNode.getRhs().accept(this));
  }

  @Override
  public StatementNode visit(AssignNode assignNode) {
    Reference newLhs = assignNode.getLhs().accept(this);
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return AssignNode.create(newLhs, newRhs);
  }

  @Override
  public StatementNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    StatementNode newThen = ifNode.getThenBranch().accept(this);
    StatementNode newElse = ifNode.getElseBranch().accept(this);
    return IfNode.create(newGuard, newThen, newElse);
  }

  @Override
  public StatementNode visit(WhileNode whileNode) {
    ExpressionNode newGuard = whileNode.getGuard().accept(this);
    StatementNode newBody = whileNode.getBody().accept(this);
    return WhileNode.create(newGuard, newBody);
  }

  @Override
  public StatementNode visit(ForNode forNode) {
    StatementNode newInit = forNode.getInitialize().accept(this);
    ExpressionNode newGuard = forNode.getGuard().accept(this);
    StatementNode newUpdate = forNode.getUpdate().accept(this);
    StatementNode newBody = forNode.getBody().accept(this);
    return ForNode.create(newInit, newGuard, newUpdate, newBody);
  }

  @Override
  public StatementNode visit(LoopNode loopNode) {
    StatementNode newBody = loopNode.getBody().accept(this);
    return LoopNode.create(newBody);
  }

  @Override
  public StatementNode visit(BreakNode breakNode) {
    ExpressionNode newLevel = breakNode.getLevel().accept(this);
    return BreakNode.create(newLevel);
  }

  @Override
  public StatementNode visit(BlockNode blockNode) {
    List<StatementNode> newList = new LinkedList<>();
    for (StatementNode stmt : blockNode) {
      newList.add(stmt.accept(this));
    }
    return BlockNode.create(newList);
  }

  @Override
  public StatementNode visit(SendNode sendNode) {
    ExpressionNode newExpr = sendNode.getSentExpression().accept(this);
    return SendNode.create(sendNode.getRecipient(), newExpr);
  }

  @Override
  public StatementNode visit(ReceiveNode receiveNode) {
    return ReceiveNode.create(
        receiveNode.getVariable(), receiveNode.getRecvType(), receiveNode.getSender());
  }

  @Override
  public ProgramNode visit(ProgramNode programNode) {
    final ProgramNode.Builder builder = ProgramNode.builder();
    try {
      for (Tuple2<ProcessName, StatementNode> process : programNode) {
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
  public StatementNode visit(AssertNode assertNode) {
    ExpressionNode newExpr = assertNode.getExpression().accept(this);
    return AssertNode.create(newExpr);
  }
}
