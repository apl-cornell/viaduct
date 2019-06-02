package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualToNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import io.vavr.Tuple;
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
        ProcessConfigurationVisitor<ProcessConfigurationNode> {
  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    return literalNode;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    return readNode;
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
  public ExpressionNode visit(EqualToNode equalToNode) {
    ExpressionNode newLhs = equalToNode.getLhs().accept(this);
    ExpressionNode newRhs = equalToNode.getRhs().accept(this);
    return new EqualToNode(newLhs, newRhs);
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
  public ExpressionNode visit(PlusNode plusNode) {
    ExpressionNode newLhs = plusNode.getLhs().accept(this);
    ExpressionNode newRhs = plusNode.getRhs().accept(this);
    return new PlusNode(newLhs, newRhs);
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    ExpressionNode newExpr = downgradeNode.getExpression().accept(this);
    return new DowngradeNode(newExpr, downgradeNode.getLabel());
  }

  @Override
  public StmtNode visit(DeclarationNode declarationNode) {
    return new DeclarationNode(declarationNode.getVariable(), declarationNode.getLabel());
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclarationNode) {
    return new ArrayDeclarationNode(
        arrayDeclarationNode.getVariable(),
        arrayDeclarationNode.getLength(),
        arrayDeclarationNode.getLabel());
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    ExpressionNode newRhs = assignNode.getRhs().accept(this);
    return new AssignNode(assignNode.getVariable(), newRhs);
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    ExpressionNode newGuard = ifNode.getGuard().accept(this);
    StmtNode newThen = ifNode.getThenBranch().accept(this);
    StmtNode newElse = ifNode.getElseBranch().accept(this);
    return new IfNode(newGuard, newThen, newElse);
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
  public ProcessConfigurationNode visit(ProcessConfigurationNode processConfigurationNode) {
    List<Tuple2<Host, StmtNode>> newConfiguration = new LinkedList<>();
    for (Tuple2<Host, StmtNode> process : processConfigurationNode) {
      newConfiguration.add(Tuple.of(process._1, process._2.accept(this)));
    }
    return new ProcessConfigurationNode(newConfiguration);
  }
}
