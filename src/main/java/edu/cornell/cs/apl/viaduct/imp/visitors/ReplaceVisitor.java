package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.List;

// TODO: get rid of this whole class (or inherit from IdentityVisitor).

/** replaces parts of AST. */
public class ReplaceVisitor implements AstVisitor<ImpAstNode> {
  private StmtNode curStmt;
  private StmtNode newStmt;
  private ExpressionNode curExpr;
  private ExpressionNode newExpr;

  /** Replace an expression in the AST. */
  public ImpAstNode run(
      ImpAstNode ast, ExpressionNode oldExpression, ExpressionNode newExpression) {
    this.curExpr = oldExpression;
    this.newExpr = newExpression;
    this.curStmt = null;
    this.newStmt = null;
    return ast.accept(this);
  }

  /** Replace a statement in the AST. */
  public ImpAstNode run(ImpAstNode ast, StmtNode oldStatement, StmtNode newStatement) {
    this.curExpr = null;
    this.newExpr = null;
    this.curStmt = oldStatement;
    this.newStmt = newStatement;
    return ast.accept(this);
  }

  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    if (literalNode.equals(this.curExpr)) {
      return newExpr;

    } else {
      return literalNode;
    }
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    if (readNode.equals(this.curExpr)) {
      return newExpr;

    } else {
      return readNode;
    }
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    if (notNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newExpr = (ExpressionNode) notNode.getExpression().accept(this);
      return new NotNode(newExpr);
    }
  }

  @Override
  public ExpressionNode visit(BinaryExpressionNode binaryExpressionNode) {
    if (binaryExpressionNode.equals(this.curExpr)) {
      return newExpr;
    } else {
      ExpressionNode newLhs = (ExpressionNode) binaryExpressionNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) binaryExpressionNode.getRhs().accept(this);
      return BinaryExpressionNode.create(newLhs, binaryExpressionNode.getOperator(), newRhs);
    }
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    if (downgradeNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newExpr = (ExpressionNode) downgradeNode.getExpression().accept(this);
      return new DowngradeNode(newExpr, downgradeNode.getLabel());
    }
  }

  @Override
  public StmtNode visit(DeclarationNode declarationNode) {
    if (declarationNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return new DeclarationNode(declarationNode.getVariable(), declarationNode.getLabel());
    }
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclarationNode) {
    if (arrayDeclarationNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return arrayDeclarationNode;
    }
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    if (assignNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      ExpressionNode newRhs = (ExpressionNode) assignNode.getRhs().accept(this);
      return new AssignNode(assignNode.getVariable(), newRhs);
    }
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    if (sendNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      ExpressionNode newExpr = (ExpressionNode) sendNode.getSentExpression().accept(this);
      return new SendNode(sendNode.getRecipient(), newExpr);
    }
  }

  @Override
  public StmtNode visit(ReceiveNode receiveNode) {
    if (receiveNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return new ReceiveNode(receiveNode.getVariable(), receiveNode.getSender());
    }
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    if (ifNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      ExpressionNode newGuard = (ExpressionNode) ifNode.getGuard().accept(this);
      StmtNode newThen = (StmtNode) ifNode.getThenBranch().accept(this);
      StmtNode newElse = (StmtNode) ifNode.getElseBranch().accept(this);
      return new IfNode(newGuard, newThen, newElse);
    }
  }

  @Override
  public StmtNode visit(BlockNode blockNode) {
    if (blockNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      List<StmtNode> newList = new ArrayList<>();
      for (StmtNode stmt : blockNode) {
        newList.add((StmtNode) stmt.accept(this));
      }
      return new BlockNode(newList);
    }
  }

  @Override
  public ProcessConfigurationNode visit(ProcessConfigurationNode processConfigurationNode) {
    List<Tuple2<Host, StmtNode>> newConfiguration = new ArrayList<>();
    for (Tuple2<Host, StmtNode> process : processConfigurationNode) {
      newConfiguration.add(Tuple.of(process._1, (StmtNode) process._2.accept(this)));
    }
    return new ProcessConfigurationNode(newConfiguration);
  }

  @Override
  public StmtNode visit(AssertNode assertNode) {
    if (assertNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      ExpressionNode newExpr = (ExpressionNode) assertNode.getExpression().accept(this);
      return new AssertNode(newExpr);
    }
  }
}
