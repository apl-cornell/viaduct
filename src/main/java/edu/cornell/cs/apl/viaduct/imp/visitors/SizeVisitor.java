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
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
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

/** gives the size of an AST node. */
public class SizeVisitor
    implements ReferenceVisitor<Integer>,
        ExprVisitor<Integer>,
        StmtVisitor<Integer>,
        ProgramVisitor<Integer> {

  @Override
  public Integer visit(Variable variable) {
    return 1;
  }

  @Override
  public Integer visit(ArrayIndex arrayIndex) {
    return 1 + arrayIndex.getIndex().accept(this);
  }

  @Override
  public Integer visit(ReadNode readNode) {
    return readNode.getReference().accept(this);
  }

  @Override
  public Integer visit(LiteralNode literalNode) {
    return 1;
  }

  @Override
  public Integer visit(NotNode notNode) {
    return 1 + notNode.getExpression().accept(this);
  }

  @Override
  public Integer visit(BinaryExpressionNode binaryExpressionNode) {
    return 1
        + binaryExpressionNode.getLhs().accept(this)
        + binaryExpressionNode.getRhs().accept(this);
  }

  @Override
  public Integer visit(DowngradeNode downgradeNode) {
    Integer exprSize = downgradeNode.getExpression().accept(this);
    return 1 + exprSize;
  }

  @Override
  public Integer visit(VariableDeclarationNode varDecl) {
    return 1;
  }

  @Override
  public Integer visit(ArrayDeclarationNode arrayDeclarationNode) {
    return arrayDeclarationNode.getLength().accept(this);
  }

  @Override
  public Integer visit(LetBindingNode letBindingNode) {
    return letBindingNode.getRhs().accept(this) + 1;
  }

  @Override
  public Integer visit(AssignNode assignNode) {
    return 1 + assignNode.getLhs().accept(this) + assignNode.getRhs().accept(this);
  }

  @Override
  public Integer visit(SendNode sendNode) {
    return sendNode.getSentExpression().accept(this) + 1;
  }

  @Override
  public Integer visit(ReceiveNode receiveNode) {
    return 1;
  }

  @Override
  public Integer visit(IfNode ifNode) {
    Integer guardSize = ifNode.getGuard().accept(this);
    Integer thenSize = ifNode.getThenBranch().accept(this);
    Integer elseSize = ifNode.getElseBranch().accept(this);
    return 1 + guardSize + thenSize + elseSize;
  }

  @Override
  public Integer visit(WhileNode whileNode) {
    Integer guardSize = whileNode.getGuard().accept(this);
    Integer bodySize = whileNode.getBody().accept(this);
    return 1 + guardSize + bodySize;
  }

  @Override
  public Integer visit(ForNode forNode) {
    Integer initSize = forNode.getInitialize().accept(this);
    Integer guardSize = forNode.getGuard().accept(this);
    Integer updateSize = forNode.getUpdate().accept(this);
    Integer bodySize = forNode.getBody().accept(this);
    return 1 + initSize + guardSize + updateSize + bodySize;
  }

  @Override
  public Integer visit(BlockNode blockNode) {
    int size = 0;
    for (StmtNode stmt : blockNode) {
      size += stmt.accept(this);
    }
    return size;
  }

  @Override
  public Integer visit(ProgramNode programNode) {
    int size = 0;
    for (Tuple2<ProcessName, StmtNode> process : programNode) {
      size += process._2().accept(this);
    }
    return size;
  }

  @Override
  public Integer visit(AssertNode assertNode) {
    return 1 + assertNode.getExpression().accept(this);
  }
}
