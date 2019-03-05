package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.surface.AndNode;
import edu.cornell.cs.apl.viaduct.surface.AssignNode;
import edu.cornell.cs.apl.viaduct.surface.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.surface.BlockNode;
import edu.cornell.cs.apl.viaduct.surface.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.surface.DowngradeNode;
import edu.cornell.cs.apl.viaduct.surface.EqualNode;
import edu.cornell.cs.apl.viaduct.surface.IfNode;
import edu.cornell.cs.apl.viaduct.surface.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.surface.LeqNode;
import edu.cornell.cs.apl.viaduct.surface.LessThanNode;
import edu.cornell.cs.apl.viaduct.surface.NotNode;
import edu.cornell.cs.apl.viaduct.surface.OrNode;
import edu.cornell.cs.apl.viaduct.surface.PlusNode;
import edu.cornell.cs.apl.viaduct.surface.ReadNode;
import edu.cornell.cs.apl.viaduct.surface.SkipNode;
import edu.cornell.cs.apl.viaduct.surface.StmtNode;
import edu.cornell.cs.apl.viaduct.surface.VarDeclNode;
import edu.cornell.cs.apl.viaduct.surface.Variable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * build program dependency graph from AST. the visit methods return the set of PDG node on which
 * the AST node depends on (reads)
 */
public class PdgBuilderVisitor implements StmtVisitor<Set<PdgNode>>, ExprVisitor<Set<PdgNode>> {
  private SymbolTable<Variable, PdgNode> storageNodes;
  private AbstractLineNumberBuilder lineNumberBuilder;
  private ProgramDependencyGraph pdg;

  /** constructor that initializes to default "empty" state. */
  public PdgBuilderVisitor() {
    this.storageNodes = new SymbolTable<Variable, PdgNode>();
    this.lineNumberBuilder = new AbstractLineNumberBuilder();
    this.pdg = new ProgramDependencyGraph();
  }

  /** return built PDG. */
  public ProgramDependencyGraph getPdg() {
    return this.pdg;
  }

  /** visit binary operation expr. */
  private Set<PdgNode> visitBinaryOp(BinaryExpressionNode binNode) {
    Set<PdgNode> lhsDeps = binNode.getLhs().accept(this);
    Set<PdgNode> rhsDeps = binNode.getRhs().accept(this);
    Set<PdgNode> deps = new HashSet<PdgNode>(lhsDeps);
    deps.addAll(rhsDeps);
    return deps;
  }

  /** return PDG storage node for referenced var. */
  public Set<PdgNode> visit(ReadNode varLookup) {
    if (this.storageNodes.contains(varLookup.getVariable())) {
      PdgNode varNode = this.storageNodes.get(varLookup.getVariable());
      HashSet<PdgNode> deps = new HashSet<PdgNode>();
      deps.add(varNode);
      return deps;

    } else {
      throw new UndeclaredVariableException(varLookup.getVariable());
    }
  }

  /** return empty set of dependencies. */
  public Set<PdgNode> visit(IntegerLiteralNode intLit) {
    return new HashSet<PdgNode>();
  }

  /** return LHS and RHS dependencies. */
  public Set<PdgNode> visit(PlusNode plusNode) {
    return visitBinaryOp(plusNode);
  }

  /** return empty set of dependencies. */
  public Set<PdgNode> visit(BooleanLiteralNode boolLit) {
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
    Set<PdgNode> deps = new HashSet<PdgNode>(notNode.getExpression().accept(this));
    return deps;
  }

  /** return created PDG node for downgrade. */
  public Set<PdgNode> visit(DowngradeNode downgradeNode) {
    Set<PdgNode> inNodes = downgradeNode.getExpression().accept(this);
    Label label = downgradeNode.getLabel();

    // create new PDG node
    // calculate inLabel later during dataflow analysis
    AbstractLineNumber lineno = lineNumberBuilder.generateLineNumber();
    PdgNode node = new PdgComputeNode(downgradeNode, lineno, Label.BOTTOM, label);
    node.addInNodes(inNodes);

    // make sure to add outEdges from inNodes to the new node
    for (PdgNode inNode : inNodes) {
      inNode.addOutNode(node);
    }

    this.pdg.addNode(node);

    Set<PdgNode> deps = new HashSet<PdgNode>();
    deps.add(node);
    return deps;
  }

  /** return empty set of dependencies. */
  public Set<PdgNode> visit(SkipNode skipNode) {
    return new HashSet<PdgNode>();
  }

  /** return created storage node. */
  public Set<PdgNode> visit(VarDeclNode varDecl) {
    AbstractLineNumber lineno = this.lineNumberBuilder.generateLineNumber();
    PdgNode node = new PdgStorageNode(varDecl, lineno, varDecl.getLabel());
    this.storageNodes.add(varDecl.getVariable(), node);
    this.pdg.addNode(node);

    return new HashSet<PdgNode>();
  }

  /** return created PDG compute node for assignment. */
  public Set<PdgNode> visit(AssignNode assignNode) {
    if (this.storageNodes.contains(assignNode.getVariable())) {
      Set<PdgNode> inNodes = assignNode.getRhs().accept(this);
      PdgNode varNode = this.storageNodes.get(assignNode.getVariable());

      // create new PDG node for the assignment that reads from the RHS nodes
      // and writes to the variable's storage node
      AbstractLineNumber lineno = this.lineNumberBuilder.generateLineNumber();
      PdgNode node = new PdgComputeNode(assignNode, lineno, Label.BOTTOM);
      node.addInNodes(inNodes);
      node.addOutNode(varNode);

      for (PdgNode inNode : inNodes) {
        inNode.addOutNode(node);
      }
      varNode.addInNode(node);
      this.pdg.addNode(node);

      Set<PdgNode> deps = new HashSet<PdgNode>();
      deps.add(node);
      return deps;

    } else {
      throw new UndeclaredVariableException(assignNode.getVariable());
    }
  }

  /** return dependencies of list of stmts. */
  public Set<PdgNode> visit(BlockNode blockNode) {
    Set<PdgNode> deps = new HashSet<PdgNode>();
    List<StmtNode> stmts = blockNode.getStatements();
    for (StmtNode stmt : stmts) {
      Set<PdgNode> stmtDeps = stmt.accept(this);
      deps.addAll(stmtDeps);
    }

    return deps;
  }

  /** return created PDG compute node for conditional. */
  public Set<PdgNode> visit(IfNode ifNode) {
    // add edges from guard nodes
    final Set<PdgNode> inNodes = ifNode.getGuard().accept(this);

    AbstractLineNumber lineno = this.lineNumberBuilder.generateLineNumber();
    PdgNode node = new PdgControlNode(ifNode, lineno, Label.BOTTOM);
    this.pdg.addNode(node);

    // then and else branches create a new lexical scope, so
    // must push then pop a new symbol table for them

    Set<PdgNode> outNodes = new HashSet<PdgNode>();
    this.storageNodes.push();
    this.lineNumberBuilder.pushBranch(AbstractLineNumberBuilder.THEN_MARKER);
    Set<PdgNode> thenNodes = ifNode.getThenBranch().accept(this);
    outNodes.addAll(thenNodes);
    this.storageNodes.pop();
    this.lineNumberBuilder.popBranch();

    this.storageNodes.push();
    this.lineNumberBuilder.pushBranch(AbstractLineNumberBuilder.ELSE_MARKER);
    Set<PdgNode> elseNodes = ifNode.getElseBranch().accept(this);
    outNodes.addAll(elseNodes);
    this.storageNodes.pop();
    this.lineNumberBuilder.popBranch();

    // add in edges
    node.addInNodes(inNodes);
    for (PdgNode inNode : inNodes) {
      inNode.addOutNode(node);
    }

    // add out edges
    Set<PdgNode> additionalOutNodes = new HashSet<PdgNode>();
    node.addOutNodes(outNodes);
    for (PdgNode outNode : outNodes) {
      outNode.addInNode(node);

      // also, to model read channels we must add out edges to
      // all storage nodes read from the branches
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
