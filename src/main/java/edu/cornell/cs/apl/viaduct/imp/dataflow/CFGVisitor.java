package edu.cornell.cs.apl.viaduct.imp.dataflow;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** builds a CFG from an AST. */
public class CFGVisitor implements StmtVisitor<Void> {
  List<CFGNode> nodes;
  Set<CFGNode> lastNodes;
  Set<Variable> declaredVars;
  Set<Variable> tempVars;
  List<Variable> vars;

  /** constructor. */
  public CFGVisitor() {
    this.nodes = new ArrayList<>();
    this.lastNodes = new HashSet<>();
    this.declaredVars = new HashSet<>();
    this.tempVars = new HashSet<>();
    this.vars = new ArrayList<>();
  }

  /** create CFG from an AST. */
  public ControlFlowGraph createCFG(StmtNode program) {
    this.lastNodes = new HashSet<>();
    program.accept(this);
    return new ControlFlowGraph(this.nodes, this.declaredVars, this.tempVars, this.vars);
  }

  protected Void setLastNodes(CFGNode... nodes) {
    this.lastNodes.clear();
    for (CFGNode node : nodes) {
      this.lastNodes.add(node);
    }

    return null;
  }

  protected Void visitSingleStatement(StmtNode stmt) {
    CFGNode node = new CFGNode(stmt);

    for (CFGNode lastNode : this.lastNodes) {
      node.addInNode(lastNode);
      lastNode.addOutNode(node);
    }

    setLastNodes(node);

    this.nodes.add(node);
    return null;
  }

  @Override
  public Void visit(DeclarationNode declNode) {
    Variable var = declNode.getVariable();
    this.declaredVars.add(var);
    this.vars.add(var);
    visitSingleStatement(declNode);
    return null;
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclNode) {
    Variable var = arrayDeclNode.getVariable();
    this.declaredVars.add(var);
    this.vars.add(var);
    visitSingleStatement(arrayDeclNode);
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    Variable var = assignNode.getVariable();
    if (!this.declaredVars.contains(var)) {
      this.tempVars.add(var);
      this.vars.add(var);
    }
    visitSingleStatement(assignNode);
    return null;
  }

  @Override
  public Void visit(SendNode sendNode) {
    visitSingleStatement(sendNode);
    return null;
  }

  @Override
  public Void visit(ReceiveNode recvNode) {
    Variable var = recvNode.getVariable();
    if (!this.declaredVars.contains(var)) {
      this.tempVars.add(var);
      this.vars.add(var);
    }
    visitSingleStatement(recvNode);
    return null;
  }

  @Override
  public Void visit(IfNode ifNode) {
    StmtNode newIf = new IfNode(ifNode.getGuard(), new BlockNode(), new BlockNode());
    visitSingleStatement(newIf);
    final Set<CFGNode> ifLastNodes = new HashSet<>(this.lastNodes);

    ifNode.getThenBranch().accept(this);
    final Set<CFGNode> thenLastNodes = new HashSet<>(this.lastNodes);
    this.lastNodes = ifLastNodes;

    ifNode.getElseBranch().accept(this);
    final Set<CFGNode> elseLastNodes = new HashSet<>(this.lastNodes);

    this.lastNodes.clear();
    this.lastNodes.addAll(thenLastNodes);
    this.lastNodes.addAll(elseLastNodes);

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
  public Void visit(AssertNode assertNode) {
    return visitSingleStatement(assertNode);
  }
}
