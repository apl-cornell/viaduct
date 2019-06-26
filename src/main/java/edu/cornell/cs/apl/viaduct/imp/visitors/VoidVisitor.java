package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import io.vavr.Tuple2;

/**
 * A visitor that traverses the AST but does nothing.
 *
 * <p>Can be subclassed to do something for specific AST nodes.
 */
public class VoidVisitor
    implements ReferenceVisitor<Void>, ExprVisitor<Void>, StmtVisitor<Void>, ProgramVisitor<Void> {

  @Override
  public Void visit(Variable variable) {
    return null;
  }

  @Override
  public Void visit(ArrayIndex arrayIndex) {
    arrayIndex.getIndex().accept(this);
    return null;
  }

  @Override
  public Void visit(LiteralNode literalNode) {
    return null;
  }

  @Override
  public Void visit(ReadNode readNode) {
    return null;
  }

  @Override
  public Void visit(NotNode notNode) {
    notNode.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(BinaryExpressionNode binaryExpressionNode) {
    binaryExpressionNode.getLhs().accept(this);
    binaryExpressionNode.getRhs().accept(this);
    return null;
  }

  @Override
  public Void visit(DowngradeNode downgradeNode) {
    downgradeNode.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(VariableDeclarationNode variableDeclarationNode) {
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclarationNode) {
    arrayDeclarationNode.getLength().accept(this);
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    assignNode.getLhs().accept(this);
    assignNode.getRhs().accept(this);
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    sendNode.getSentExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    ifNode.getGuard().accept(this);
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);
    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    whileNode.getGuard().accept(this);
    whileNode.getBody().accept(this);
    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    forNode.getInitialize().accept(this);
    forNode.getGuard().accept(this);
    forNode.getUpdate().accept(this);
    forNode.getBody().accept(this);
    return null;
  }

  @Override
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode) {
      stmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(ProgramNode programNode) {
    for (Tuple2<ProcessName, StmtNode> process : programNode) {
      process._2.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    assertNode.getExpression().accept(this);
    return null;
  }
}
