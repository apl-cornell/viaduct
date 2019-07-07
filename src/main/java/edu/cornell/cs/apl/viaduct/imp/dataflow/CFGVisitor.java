package edu.cornell.cs.apl.viaduct.imp.dataflow;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** builds a CFG from an AST. */
public class CFGVisitor implements StmtVisitor<Void> {
  private List<CFGNode> nodes;
  private Set<CFGNode> lastNodes;
  private Set<Variable> declaredVars;
  private Set<Variable> tempVars;
  private List<Variable> vars;

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
    Collections.addAll(this.lastNodes, nodes);

    return null;
  }

  protected CFGNode visitSingleStatement(StmtNode stmt) {
    CFGNode node = new CFGNode(stmt);

    for (CFGNode lastNode : this.lastNodes) {
      node.addInNode(lastNode);
      lastNode.addOutNode(node);
    }

    this.nodes.add(node);
    setLastNodes(node);
    return node;
  }

  @Override
  public Void visit(VariableDeclarationNode declNode) {
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
  public Void visit(LetBindingNode letBindingNode) {
    Variable var = letBindingNode.getVariable();
    this.tempVars.add(var);
    this.vars.add(var);
    visitSingleStatement(letBindingNode);
    return null;
  }

  @Override
  public Void visit(AssignNode assignNode) {
    Reference lhs = assignNode.getLhs();

    if (lhs instanceof Variable) {
      Variable var = (Variable) lhs;
      if (!this.declaredVars.contains(var)) {
        this.tempVars.add(var);
        this.vars.add(var);
      }
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
  public Void visit(WhileNode whileNode) {
    // TODO: throw elaboration exception instead
    StmtNode newWhile = new WhileNode(whileNode.getGuard(), new BlockNode());
    CFGNode whileCfg = visitSingleStatement(newWhile);

    whileNode.getBody().accept(this);
    final Set<CFGNode> bodyLastNodes = new HashSet<>(this.lastNodes);

    for (CFGNode bodyLastNode : bodyLastNodes) {
      bodyLastNode.addOutNode(whileCfg);
      whileCfg.addInNode(bodyLastNode);
    }

    this.lastNodes.clear();
    this.lastNodes.addAll(bodyLastNodes);

    return null;
  }

  @Override
  public Void visit(ForNode forNode) {
    throw new Error(new ElaborationException());
  }

  @Override
  public Void visit(LoopNode loopNode) {
    StmtNode newLoop = new LoopNode(new BlockNode());
    CFGNode loopCfg = visitSingleStatement(newLoop);

    loopNode.getBody().accept(this);
    final Set<CFGNode> bodyLastNodes = new HashSet<>(this.lastNodes);

    for (CFGNode bodyLastNode : bodyLastNodes) {
      bodyLastNode.addOutNode(loopCfg);
      loopCfg.addInNode(bodyLastNode);
    }

    this.lastNodes.clear();
    this.lastNodes.addAll(bodyLastNodes);

    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    visitSingleStatement(breakNode);
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
    // visitSingleStatement(assertNode);
    return null;
  }
}
