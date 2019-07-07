package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import java.util.HashMap;
import java.util.Map;

/** replaces parts of AST. */
public class ReplaceVisitor extends IdentityVisitor {

  private final Map<StmtNode, StmtNode> stmtMap;
  private final Map<ExpressionNode, ExpressionNode> exprMap;

  public ReplaceVisitor() {
    this.stmtMap = new HashMap<>();
    this.exprMap = new HashMap<>();
  }

  public ReplaceVisitor(Map<ExpressionNode, ExpressionNode> emap, Map<StmtNode, StmtNode> smap) {
    this.exprMap = emap;
    this.stmtMap = smap;
  }

  /** build visitor from substitution of vars. */
  public ReplaceVisitor(Map<Variable, ExpressionNode> vmap) {
    this.exprMap = new HashMap<>();
    this.stmtMap = new HashMap<>();

    for (Map.Entry<Variable, ExpressionNode> kv : vmap.entrySet()) {
      this.exprMap.put(new ReadNode(kv.getKey()), kv.getValue());
    }
  }

  private ImpAstNode run(ImpAstNode ast) {
    if (ast instanceof ExpressionNode) {
      return ((ExpressionNode) ast).accept(this);

    } else {
      return ((StmtNode) ast).accept(this);
    }
  }

  /** Replace an expression in the AST. */
  public ImpAstNode run(ImpAstNode ast, ExpressionNode oldExpr, ExpressionNode newExpr) {
    this.exprMap.put(oldExpr, newExpr);
    return run(ast);
  }

  /** Replace a statement in the AST. */
  public ImpAstNode run(ImpAstNode ast, StmtNode oldStmt, StmtNode newStmt) {
    this.stmtMap.put(oldStmt, newStmt);
    return run(ast);
  }

  @Override
  public ExpressionNode visit(LiteralNode literalNode) {
    if (this.exprMap.containsKey(literalNode)) {
      return this.exprMap.get(literalNode);

    } else {
      return super.visit(literalNode);
    }
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    if (this.exprMap.containsKey(readNode)) {
      return this.exprMap.get(readNode);

    } else {
      return super.visit(readNode);
    }
  }

  @Override
  public ExpressionNode visit(NotNode notNode) {
    if (this.exprMap.containsKey(notNode)) {
      return this.exprMap.get(notNode);

    } else {
      return super.visit(notNode);
    }
  }

  @Override
  public ExpressionNode visit(BinaryExpressionNode binaryExpressionNode) {
    if (this.exprMap.containsKey(binaryExpressionNode)) {
      return this.exprMap.get(binaryExpressionNode);

    } else {
      return super.visit(binaryExpressionNode);
    }
  }

  @Override
  public ExpressionNode visit(DowngradeNode downgradeNode) {
    if (this.exprMap.containsKey(downgradeNode)) {
      return this.exprMap.get(downgradeNode);

    } else {
      return super.visit(downgradeNode);
    }
  }

  @Override
  public StmtNode visit(VariableDeclarationNode variableDeclarationNode) {
    if (this.stmtMap.containsKey(variableDeclarationNode)) {
      return this.stmtMap.get(variableDeclarationNode);

    } else {
      return super.visit(variableDeclarationNode);
    }
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclarationNode) {
    if (this.stmtMap.containsKey(arrayDeclarationNode)) {
      return this.stmtMap.get(arrayDeclarationNode);

    } else {
      return super.visit(arrayDeclarationNode);
    }
  }

  @Override
  public StmtNode visit(LetBindingNode letBindingNode) {
    if (this.stmtMap.containsKey(letBindingNode)) {
      return this.stmtMap.get(letBindingNode);

    } else {
      return super.visit(letBindingNode);
    }
  }

  @Override
  public StmtNode visit(AssignNode assignNode) {
    if (this.stmtMap.containsKey(assignNode)) {
      return this.stmtMap.get(assignNode);

    } else {
      return super.visit(assignNode);
    }
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    if (this.stmtMap.containsKey(sendNode)) {
      return this.stmtMap.get(sendNode);

    } else {
      return super.visit(sendNode);
    }
  }

  @Override
  public StmtNode visit(ReceiveNode receiveNode) {
    if (this.stmtMap.containsKey(receiveNode)) {
      return this.stmtMap.get(receiveNode);

    } else {
      return super.visit(receiveNode);
    }
  }

  @Override
  public StmtNode visit(IfNode ifNode) {
    if (this.stmtMap.containsKey(ifNode)) {
      return this.stmtMap.get(ifNode);

    } else {
      return super.visit(ifNode);
    }
  }

  @Override
  public StmtNode visit(WhileNode whileNode) {
    if (this.stmtMap.containsKey(whileNode)) {
      return this.stmtMap.get(whileNode);

    } else {
      return super.visit(whileNode);
    }
  }

  @Override
  public StmtNode visit(ForNode forNode) {
    if (this.stmtMap.containsKey(forNode)) {
      return this.stmtMap.get(forNode);

    } else {
      return super.visit(forNode);
    }
  }

  @Override
  public StmtNode visit(LoopNode loopNode) {
    if (this.stmtMap.containsKey(loopNode)) {
      return this.stmtMap.get(loopNode);

    } else {
      return super.visit(loopNode);
    }
  }

  @Override
  public StmtNode visit(BreakNode breakNode) {
    if (this.stmtMap.containsKey(breakNode)) {
      return this.stmtMap.get(breakNode);

    } else {
      return super.visit(breakNode);
    }
  }

  @Override
  public StmtNode visit(BlockNode blockNode) {
    if (this.stmtMap.containsKey(blockNode)) {
      return this.stmtMap.get(blockNode);

    } else {
      return super.visit(blockNode);
    }
  }

  @Override
  public StmtNode visit(AssertNode assertNode) {
    if (this.stmtMap.containsKey(assertNode)) {
      return this.stmtMap.get(assertNode);

    } else {
      return super.visit(assertNode);
    }
  }
}
