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
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
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

/** replaces parts of AST. */
public class ReplaceVisitor implements AstVisitor<ImpAstNode> {
  StmtNode curStmt;
  StmtNode newStmt;
  ExpressionNode curExpr;
  ExpressionNode newExpr;

  /** replace an expression in the AST. */
  public ImpAstNode replaceExpr(ImpAstNode ast, ExpressionNode cexpr, ExpressionNode nexpr) {
    this.curExpr = cexpr;
    this.newExpr = nexpr;
    this.curStmt = null;
    this.newStmt = null;
    return ast.accept(this);
  }

  /** replace a statement in the AST. */
  public ImpAstNode replaceStmt(ImpAstNode ast, StmtNode cstmt, StmtNode nstmt) {
    this.curExpr = null;
    this.newExpr = null;
    this.curStmt = cstmt;
    this.newStmt = nstmt;
    return ast.accept(this);
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    if (readNode.equals(this.curExpr)) {
      return newExpr;

    } else {
      return new ReadNode(readNode.getVariable());
    }
  }

  @Override
  public ExpressionNode visit(IntegerLiteralNode integerLiteralNode) {
    if (integerLiteralNode.equals(this.curExpr)) {
      return newExpr;

    } else {
      return new IntegerLiteralNode(integerLiteralNode.getValue());
    }
  }

  @Override
  public ExpressionNode visit(PlusNode plusNode) {
    if (plusNode.equals(this.curExpr)) {
      return newExpr;

    } else {
      ExpressionNode newLhs = (ExpressionNode) plusNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) plusNode.getRhs().accept(this);
      return new PlusNode(newLhs, newRhs);
    }
  }

  @Override
  public ExpressionNode visit(BooleanLiteralNode booleanLiteralNode) {
    if (booleanLiteralNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      return new BooleanLiteralNode(booleanLiteralNode.getValue());
    }
  }

  @Override
  public ExpressionNode visit(OrNode orNode) {
    if (orNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newLhs = (ExpressionNode) orNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) orNode.getRhs().accept(this);
      return new OrNode(newLhs, newRhs);
    }
  }

  @Override
  public ExpressionNode visit(AndNode andNode) {
    if (andNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newLhs = (ExpressionNode) andNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) andNode.getRhs().accept(this);
      return new AndNode(newLhs, newRhs);
    }
  }

  @Override
  public ExpressionNode visit(LessThanNode lessThanNode) {
    if (lessThanNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newLhs = (ExpressionNode) lessThanNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) lessThanNode.getRhs().accept(this);
      return new LessThanNode(newLhs, newRhs);
    }
  }

  @Override
  public ExpressionNode visit(EqualNode equalNode) {
    if (equalNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newLhs = (ExpressionNode) equalNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) equalNode.getRhs().accept(this);
      return new EqualNode(newLhs, newRhs);
    }
  }

  @Override
  public ExpressionNode visit(LeqNode leqNode) {
    if (leqNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newLhs = (ExpressionNode) leqNode.getLhs().accept(this);
      ExpressionNode newRhs = (ExpressionNode) leqNode.getRhs().accept(this);
      return new LeqNode(newLhs, newRhs);
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
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    if (downgradeNode.equals(this.curExpr)) {
      return this.newExpr;

    } else {
      ExpressionNode newExpr = (ExpressionNode) downgradeNode.getExpression().accept(this);
      return new DowngradeNode(newExpr, downgradeNode.getLabel());
    }
  }

  @Override
  public StmtNode visit(SkipNode skipNode) {
    if (skipNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return new SkipNode();
    }
  }

  @Override
  public StmtNode visit(VarDeclNode varDecl) {
    if (varDecl.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return new VarDeclNode(varDecl.getVariable(), varDecl.getLabel());
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

  /** give traverse children and do nothing. */
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

  /** traverse children and do nothing. */
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
  public StmtNode visit(SendNode sendNode) {
    if (sendNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      ExpressionNode newExpr = (ExpressionNode) sendNode.getSentExpr().accept(this);
      return new SendNode(sendNode.getRecipient(), newExpr);
    }
  }

  @Override
  public StmtNode visit(RecvNode recvNode) {
    if (recvNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return new RecvNode(recvNode.getSender(), recvNode.getVar());
    }
  }

  @Override
  public StmtNode visit(AnnotationNode annotNode) {
    if (annotNode.equals(this.curStmt)) {
      return this.newStmt;

    } else {
      return new AnnotationNode(annotNode.getAnnotationString(), annotNode.getAnnotation());
    }
  }
}
