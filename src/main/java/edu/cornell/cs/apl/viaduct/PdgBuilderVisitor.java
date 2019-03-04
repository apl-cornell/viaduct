package edu.cornell.cs.apl.viaduct;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * build program dependency graph from AST. the visit methods return the set of PDG node on which
 * the AST node depends on (reads)
 */
public class PdgBuilderVisitor implements StmtVisitor<Set<PdgNode>>, ExprVisitor<Set<PdgNode>> {
  SymbolTable<Variable, PdgNode> storageNodes;
  ProgramDependencyGraph pdg;

  public PdgBuilderVisitor() {
    this.storageNodes = new SymbolTable<Variable, PdgNode>();
    this.pdg = new ProgramDependencyGraph();
  }

  /** return built PDG. */
  public ProgramDependencyGraph getPdg() {
    return this.pdg;
  }

  /** visit binary operation expr. */
  protected Set<PdgNode> visitBinaryOp(BinaryExprNode binNode) {
    Set<PdgNode> lhsDeps = binNode.getLhs().accept(this);
    Set<PdgNode> rhsDeps = binNode.getLhs().accept(this);
    Set<PdgNode> deps = new HashSet<PdgNode>(lhsDeps);
    deps.addAll(rhsDeps);
    return deps;
  }

  /** visit declassify and endorse nodes. return the created PDG for the downgraded expr */
  public Set<PdgNode> visitDowngrade(
      AstNode downgradeNode, Set<PdgNode> inNodes, Label downgradeLabel) {
    // create new PDG node
    // calculate inLabel later during dataflow analysis
    PdgNode node = new PdgComputeNode(downgradeNode, Label.BOTTOM, downgradeLabel);
    node.addInNodes(inNodes);

    // make sure to add outEdges from inNodes to the new node
    for (PdgNode inNode : inNodes) {
      inNode.addOutNode(node);
    }

    this.pdg.add(node);

    Set<PdgNode> deps = new HashSet<PdgNode>();
    deps.add(node);
    return deps;
  }

  /** return PDG storage node for referenced var. */
  public Set<PdgNode> visit(VarLookupNode varLookup) {
    if (this.storageNodes.contains(varLookup.getVar())) {
      PdgNode varNode = this.storageNodes.get(varLookup.getVar());
      HashSet<PdgNode> deps = new HashSet<PdgNode>();
      deps.add(varNode);
      return deps;

    } else {
      throw new UndeclaredVariableException(varLookup.getVar());
    }
  }

  /** return empty set of dependencies. */
  public Set<PdgNode> visit(IntLiteralNode intLit) {
    return new HashSet<PdgNode>();
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(PlusNode plusNode) {
    return visitBinaryOp(plusNode);
  }

  /** return empty set of dependencies. */
  public Set<PdgNode> visit(BoolLiteralNode boolLit) {
    return new HashSet<PdgNode>();
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(OrNode orNode) {
    return visitBinaryOp(orNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(AndNode andNode) {
    return visitBinaryOp(andNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(LessThanNode ltNode) {
    return visitBinaryOp(ltNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(EqualNode eqNode) {
    return visitBinaryOp(eqNode);
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(LeqNode leqNode) {
    return visitBinaryOp(leqNode);
  }

  /** return negated expr dependencies. */
  public Set<PdgNode> visit(NotNode notNode) {
    Set<PdgNode> deps = new HashSet<PdgNode>(notNode.getNegatedExpr().accept(this));
    return deps;
  }

  /** return created PDG node for downgrade. */
  public Set<PdgNode> visit(DeclassifyNode declNode) {
    return visitDowngrade(
        declNode, declNode.getDeclassifiedExpr().accept(this), declNode.getDowngradeLabel());
  }

  /** return created PDG node for downgrade. */
  public Set<PdgNode> visit(EndorseNode endoNode) {
    return visitDowngrade(
        endoNode, endoNode.getEndorsedExpr().accept(this), endoNode.getDowngradeLabel());
  }

  /** return empty set of dependencies. */
  public Set<PdgNode> visit(SkipNode skipNode) {
    return new HashSet<PdgNode>();
  }

  /** return created storage node. */
  public Set<PdgNode> visit(VarDeclNode varDecl) {
    PdgNode node = new PdgStorageNode(varDecl, varDecl.getVarLabel());
    this.storageNodes.add(varDecl.getDeclaredVar(), node);
    this.pdg.add(node);

    return new HashSet<PdgNode>();
  }

  /** return created PDG compute node for assignment. */
  public Set<PdgNode> visit(AssignNode assignNode) {
    if (this.storageNodes.contains(assignNode.getVar())) {
      Set<PdgNode> inNodes = assignNode.getRhs().accept(this);
      PdgNode varNode = this.storageNodes.get(assignNode.getVar());

      // create new PDG node for the assignment that reads from the RHS nodes
      // and writes to the variable's storage node
      PdgNode node = new PdgComputeNode(assignNode, Label.BOTTOM);
      node.addInNodes(inNodes);
      node.addOutNode(varNode);

      for (PdgNode inNode : inNodes) {
        inNode.addOutNode(node);
      }
      varNode.addInNode(node);
      this.pdg.add(node);

      Set<PdgNode> deps = new HashSet<PdgNode>();
      deps.add(node);
      return deps;

    } else {
      throw new UndeclaredVariableException(assignNode.getVar());
    }
  }

  /** return dependencies of list of stmts. */
  public Set<PdgNode> visit(SeqNode seqNode) {
    Set<PdgNode> deps = new HashSet<PdgNode>();
    List<StmtNode> stmts = seqNode.getStmts();
    for (StmtNode stmt : stmts) {
      Set<PdgNode> stmtDeps = stmt.accept(this);
      deps.addAll(stmtDeps);
    }

    return deps;
  }

  /** return created PDG compute node for conditional. */
  public Set<PdgNode> visit(IfNode ifNode) {
    PdgNode node = new PdgComputeNode(ifNode, Label.BOTTOM);
    Set<PdgNode> inNodes = ifNode.getGuard().accept(this);
    node.addInNodes(inNodes);

    // then and else branches create a new lexical scope, so
    // must push then pop a new symbol table for them

    this.storageNodes.push();
    Set<PdgNode> thenNodes = ifNode.getThenBranch().accept(this);
    this.storageNodes.pop();

    this.storageNodes.push();
    Set<PdgNode> elseNodes = ifNode.getElseBranch().accept(this);
    this.storageNodes.pop();

    Set<PdgNode> outNodes = new HashSet<PdgNode>(thenNodes);
    outNodes.addAll(elseNodes);

    // PDG node for conditional changes PC label, so it must write
    // to storage nodes created in the then and else branches
    // also, to model read channels we must add out edges to
    // all storage nodes read from the branches
    node.addOutNodes(outNodes);

    for (PdgNode inNode : inNodes) {
      inNode.addOutNode(node);
    }

    Set<PdgNode> additionalOutNodes = new HashSet<PdgNode>();
    for (PdgNode outNode : outNodes) {
      outNode.addInNode(node);

      for (PdgNode outStorage : outNode.getStorageNodeInputs()) {
        outStorage.addInNode(node);
        additionalOutNodes.add(outStorage);
      }
    }
    node.addOutNodes(additionalOutNodes);

    Set<PdgNode> deps = new HashSet<PdgNode>();
    deps.add(node);
    return deps;
  }
}
