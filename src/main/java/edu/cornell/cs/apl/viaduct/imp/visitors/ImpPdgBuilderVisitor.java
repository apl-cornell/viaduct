package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.AbstractLineNumber;
import edu.cornell.cs.apl.viaduct.Label;
import edu.cornell.cs.apl.viaduct.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.PdgControlNode;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.SymbolTable;
import edu.cornell.cs.apl.viaduct.UndeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
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
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * build program dependency graph from AST. the visit methods return the set of PDG node on which
 * the AST node depends on (reads)
 */
public class ImpPdgBuilderVisitor
    implements StmtVisitor<Set<PdgNode<ImpAstNode>>>, ExprVisitor<Set<PdgNode<ImpAstNode>>> {

  private static final String MAIN_MARKER = "main";
  private static final String THEN_MARKER = "then";
  private static final String ELSE_MARKER = "else";

  private SymbolTable<Variable, PdgNode<ImpAstNode>> storageNodes;
  private AbstractLineNumber currLineNumber;
  private ProgramDependencyGraph<ImpAstNode> pdg;

  /** constructor that initializes to default "empty" state. */
  public ImpPdgBuilderVisitor() {
    this.storageNodes = new SymbolTable<Variable, PdgNode<ImpAstNode>>();
    this.currLineNumber = new AbstractLineNumber(MAIN_MARKER);
    this.pdg = new ProgramDependencyGraph<ImpAstNode>();
  }

  private AbstractLineNumber nextLineNumber() {
    AbstractLineNumber old = this.currLineNumber;
    this.currLineNumber = this.currLineNumber.increment();
    return old;
  }

  /** return built PDG. */
  public ProgramDependencyGraph<ImpAstNode> getPdg() {
    return this.pdg;
  }

  /** visit binary operation expr. */
  private Set<PdgNode<ImpAstNode>> visitBinaryOp(BinaryExpressionNode binNode) {
    Set<PdgNode<ImpAstNode>> lhsDeps = binNode.getLhs().accept(this);
    Set<PdgNode<ImpAstNode>> rhsDeps = binNode.getRhs().accept(this);
    Set<PdgNode<ImpAstNode>> deps = new HashSet<PdgNode<ImpAstNode>>(lhsDeps);
    deps.addAll(rhsDeps);
    return deps;
  }

  /** return PDG storage node for referenced var. */
  public Set<PdgNode<ImpAstNode>> visit(ReadNode varLookup) {
    if (this.storageNodes.contains(varLookup.getVariable())) {
      PdgNode<ImpAstNode> varNode = this.storageNodes.get(varLookup.getVariable());
      HashSet<PdgNode<ImpAstNode>> deps = new HashSet<PdgNode<ImpAstNode>>();
      deps.add(varNode);
      return deps;

    } else {
      throw new UndeclaredVariableException(varLookup.getVariable());
    }
  }

  /** return empty set of dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(IntegerLiteralNode intLit) {
    return new HashSet<PdgNode<ImpAstNode>>();
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(PlusNode plusNode) {
    return visitBinaryOp(plusNode);
  }

  /** return empty set of dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(BooleanLiteralNode boolLit) {
    return new HashSet<PdgNode<ImpAstNode>>();
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(OrNode orNode) {
    return visitBinaryOp(orNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(AndNode andNode) {
    return visitBinaryOp(andNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(LessThanNode ltNode) {
    return visitBinaryOp(ltNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(EqualNode eqNode) {
    return visitBinaryOp(eqNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(LeqNode leqNode) {
    return visitBinaryOp(leqNode);
  }

  /** return negated expr dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(NotNode notNode) {
    Set<PdgNode<ImpAstNode>> deps =
        new HashSet<PdgNode<ImpAstNode>>(notNode.getExpression().accept(this));

    return deps;
  }

  /** return created PDG node for downgrade. */
  public Set<PdgNode<ImpAstNode>> visit(DowngradeNode downgradeNode) {
    Set<PdgNode<ImpAstNode>> inNodes = downgradeNode.getExpression().accept(this);
    Label label = downgradeNode.getLabel();

    // create new PDG node
    // calculate inLabel later during dataflow analysis
    AbstractLineNumber lineno = nextLineNumber();

    PdgNode<ImpAstNode> node =
        new PdgComputeNode<ImpAstNode>(downgradeNode, lineno, Label.bottom(), label);
    node.addInNodes(inNodes);

    // make sure to add outEdges from inNodes to the new node
    for (PdgNode<ImpAstNode> inNode : inNodes) {
      inNode.addOutNode(node);
    }

    this.pdg.addNode(node);

    Set<PdgNode<ImpAstNode>> deps = new HashSet<PdgNode<ImpAstNode>>();
    deps.add(node);
    return deps;
  }

  /** return empty set of dependencies. */
  public Set<PdgNode<ImpAstNode>> visit(SkipNode skipNode) {
    return new HashSet<PdgNode<ImpAstNode>>();
  }

  /** return created storage node. */
  public Set<PdgNode<ImpAstNode>> visit(VarDeclNode varDecl) {
    AbstractLineNumber lineno = nextLineNumber();
    PdgNode<ImpAstNode> node = new PdgStorageNode<ImpAstNode>(varDecl, lineno, varDecl.getLabel());
    this.storageNodes.add(varDecl.getVariable(), node);
    this.pdg.addNode(node);

    return new HashSet<PdgNode<ImpAstNode>>();
  }

  /** return created PDG compute node for assignment. */
  public Set<PdgNode<ImpAstNode>> visit(AssignNode assignNode) {
    if (this.storageNodes.contains(assignNode.getVariable())) {
      Set<PdgNode<ImpAstNode>> inNodes = assignNode.getRhs().accept(this);
      PdgNode<ImpAstNode> varNode = this.storageNodes.get(assignNode.getVariable());

      // create new PDG node for the assignment that reads from the RHS nodes
      // and writes to the variable's storage node
      AbstractLineNumber lineno = nextLineNumber();
      PdgNode<ImpAstNode> node = new PdgComputeNode<ImpAstNode>(assignNode, lineno, Label.bottom());
      node.addInNodes(inNodes);
      node.addOutNode(varNode);

      for (PdgNode<ImpAstNode> inNode : inNodes) {
        inNode.addOutNode(node);
      }
      varNode.addInNode(node);
      this.pdg.addNode(node);

      Set<PdgNode<ImpAstNode>> deps = new HashSet<PdgNode<ImpAstNode>>();
      deps.add(node);
      return deps;

    } else {
      throw new UndeclaredVariableException(assignNode.getVariable());
    }
  }

  /** return dependencies of list of stmts. */
  public Set<PdgNode<ImpAstNode>> visit(BlockNode blockNode) {
    Set<PdgNode<ImpAstNode>> deps = new HashSet<PdgNode<ImpAstNode>>();
    List<StmtNode> stmts = blockNode.getStatements();
    for (StmtNode stmt : stmts) {
      Set<PdgNode<ImpAstNode>> stmtDeps = stmt.accept(this);
      deps.addAll(stmtDeps);
    }

    return deps;
  }

  /** return created PDG compute node for conditional. */
  public Set<PdgNode<ImpAstNode>> visit(IfNode ifNode) {
    // add edges from guard nodes
    final Set<PdgNode<ImpAstNode>> inNodes = ifNode.getGuard().accept(this);

    AbstractLineNumber ifLineno = nextLineNumber();
    PdgNode<ImpAstNode> node = new PdgControlNode<ImpAstNode>(ifNode, ifLineno, Label.bottom());
    this.pdg.addNode(node);

    // then and else branches create a new lexical scope, so
    // must push then pop a new symbol table for them

    Set<PdgNode<ImpAstNode>> outNodes = new HashSet<PdgNode<ImpAstNode>>();
    this.storageNodes.push();
    this.currLineNumber = this.currLineNumber.addBranch(THEN_MARKER);
    Set<PdgNode<ImpAstNode>> thenNodes = ifNode.getThenBranch().accept(this);
    outNodes.addAll(thenNodes);
    this.storageNodes.pop();
    this.currLineNumber = ifLineno;

    this.storageNodes.push();
    this.currLineNumber = this.currLineNumber.addBranch(ELSE_MARKER);
    Set<PdgNode<ImpAstNode>> elseNodes = ifNode.getElseBranch().accept(this);
    outNodes.addAll(elseNodes);
    this.storageNodes.pop();
    this.currLineNumber = ifLineno;

    // add in edges
    node.addInNodes(inNodes);
    for (PdgNode<ImpAstNode> inNode : inNodes) {
      inNode.addOutNode(node);
    }

    // add out edges
    Set<PdgNode<ImpAstNode>> additionalOutNodes = new HashSet<PdgNode<ImpAstNode>>();
    node.addOutNodes(outNodes);
    for (PdgNode<ImpAstNode> outNode : outNodes) {
      outNode.addInNode(node);

      // also, to model read channels we must add out edges to
      // all storage nodes read from the branches
      for (PdgNode<ImpAstNode> outStorage : outNode.getStorageNodeInputs()) {
        outStorage.addInNode(node);
        additionalOutNodes.add(outStorage);
      }
    }
    node.addOutNodes(additionalOutNodes);

    Set<PdgNode<ImpAstNode>> deps = new HashSet<PdgNode<ImpAstNode>>();
    deps.add(node);
    return deps;
  }

  /** send/recvs should not be in surface programs and thus should not be in the generated PDG. */
  public Set<PdgNode<ImpAstNode>> visit(SendNode sendNode) {
    return new HashSet<PdgNode<ImpAstNode>>();
  }

  /** send/recvs should not be in surface programs and thus should not be in the generated PDG. */
  public Set<PdgNode<ImpAstNode>> visit(RecvNode recvNode) {
    return new HashSet<PdgNode<ImpAstNode>>();
  }
}
