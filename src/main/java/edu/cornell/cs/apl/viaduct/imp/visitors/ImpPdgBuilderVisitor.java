package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
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
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgPcFlowEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgQueryEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadChannelEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** build a PDG from an IMP program. */
public class ImpPdgBuilderVisitor implements StmtVisitor<Set<PdgNode<ImpAstNode>>> {

  private static final String VARDECL_NODE = "decl";
  private static final String ASSIGN_NODE = "assgn";
  private static final String SEND_NODE = "send";
  private static final String RECV_NODE = "recv";
  private static final String IF_NODE = "if";
  private static final String LOOP_NODE = "loop";
  private static final String BREAK_NODE = "break";

  private final FreshNameGenerator nameGenerator = new FreshNameGenerator();
  private final SymbolTable<Variable, Boolean> declaredVars;
  private final SymbolTable<Variable, String> varDeclMap;
  private final Map<String, PdgNode<ImpAstNode>> nodeMap;
  private final Map<Variable, Variable> downgradeMap;
  private final QuerySetVisitor querySetVisitor;
  private final TempSetVisitor tempSetVisitor;
  private ProgramDependencyGraph<ImpAstNode> pdg;

  /** constructor. */
  public ImpPdgBuilderVisitor() {
    this.declaredVars = new SymbolTable<>();
    this.varDeclMap = new SymbolTable<>();
    this.nodeMap = new HashMap<>();
    this.querySetVisitor = new QuerySetVisitor();
    this.tempSetVisitor = new TempSetVisitor();
    this.downgradeMap = new HashMap<>();
  }

  /** generate a PDG from a program. */
  public ProgramDependencyGraph<ImpAstNode> generatePDG(StatementNode program) {
    this.declaredVars.clear();
    this.varDeclMap.clear();
    this.nodeMap.clear();
    this.pdg = new ProgramDependencyGraph<>();
    program.accept(this);
    return this.pdg;
  }

  private Set<PdgNode<ImpAstNode>> addNode(
      String name, PdgNode<ImpAstNode> node, StatementNode stmt) {
    this.pdg.addNode(node);
    this.nodeMap.put(name, node);
    stmt.setId(name);

    Set<PdgNode<ImpAstNode>> createdNodes = new HashSet<>();
    createdNodes.add(node);
    return createdNodes;
  }

  private void createReadEdges(
      Set<Variable> temps, Set<ReferenceNode> queries, PdgNode<ImpAstNode> node) {

    for (Variable temp : temps) {
      PdgNode<ImpAstNode> readNode = this.nodeMap.get(temp.getBinding());
      PdgComputeEdge.create(readNode, node, temp);
    }

    for (ReferenceNode query : queries) {
      Variable queryVar =
          query.accept(
              new ReferenceVisitor<Variable>() {
                @Override
                public Variable visit(Variable var) {
                  return var;
                }

                @Override
                public Variable visit(ArrayIndexingNode arrayIndex) {
                  return arrayIndex.getArray();
                }
              });
      String readNodeName = this.varDeclMap.get(queryVar);
      PdgNode<ImpAstNode> readNode = this.nodeMap.get(readNodeName);
      PdgQueryEdge.create(readNode, node, ReadNode.builder().setReference(query).build());
    }
  }

  private PdgNode<ImpAstNode> getVariableNode(Variable var) {
    String name = this.varDeclMap.get(var);
    PdgNode<ImpAstNode> node = this.nodeMap.get(name);
    return node;
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(VariableDeclarationNode varDecl) {
    Variable var = varDecl.getVariable();
    String name = this.nameGenerator.getFreshName(VARDECL_NODE);
    PdgStorageNode<ImpAstNode> node = new PdgStorageNode<>(varDecl, name);

    this.varDeclMap.put(var, name);
    this.declaredVars.put(var, true);
    return addNode(name, node, varDecl);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(ArrayDeclarationNode arrayDecl) {
    Variable var = arrayDecl.getVariable();
    String name = this.nameGenerator.getFreshName(VARDECL_NODE);
    PdgStorageNode<ImpAstNode> node = new PdgStorageNode<>(arrayDecl, name);

    this.varDeclMap.put(var, name);
    this.declaredVars.put(var, true);

    ExpressionNode length = arrayDecl.getLength();
    Set<Variable> temps = this.tempSetVisitor.run(length);
    Set<ReferenceNode> queries = this.querySetVisitor.run(length);

    createReadEdges(temps, queries, node);

    return addNode(name, node, arrayDecl);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(LetBindingNode letBindingNode) {
    Variable letVar = letBindingNode.getVariable();
    String name = letVar.getBinding();
    ExpressionNode rhs = letBindingNode.getRhs();

    PdgComputeNode<ImpAstNode> node = new PdgComputeNode<>(rhs, name);
    Set<Variable> temps = this.tempSetVisitor.run(rhs);
    Set<ReferenceNode> queries = this.querySetVisitor.run(rhs);
    createReadEdges(temps, queries, node);

    // in A-normal form, there should be at most one query
    // assert queries.size() <= 1;

    if (letBindingNode.isArrayIndex()) {
      node.setArrayIndex(true);
    }

    return addNode(name, node, letBindingNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(AssignNode assignNode) {
    ReferenceNode lhs = assignNode.getLhs();
    ExpressionNode rhs = assignNode.getRhs();

    Set<Variable> lhsTemps = this.tempSetVisitor.run(lhs);
    Set<Variable> rhsTemps = this.tempSetVisitor.run(rhs);
    Set<Variable> temps = new HashSet<>(lhsTemps);
    temps.addAll(rhsTemps);

    Set<ReferenceNode> queries = new HashSet<>();
    Set<ReferenceNode> lhsQueries = this.querySetVisitor.run(lhs);
    Set<ReferenceNode> rhsQueries  = this.querySetVisitor.run(rhs);
    queries.addAll(lhsQueries);
    queries.addAll(rhsQueries);
    queries.remove(lhs);

    // there should be NO queries for assignments in A-normal form!
    assert queries.size() == 0;

    String name = this.nameGenerator.getFreshName(ASSIGN_NODE);
    PdgComputeNode<ImpAstNode> node = new PdgComputeNode<>(assignNode, name);

    createReadEdges(temps, queries, node);

    // add write edge
    lhs.accept(
        new ReferenceVisitor<Void>() {
          @Override
          public Void visit(Variable var) {
            PdgNode<ImpAstNode> varNode = ImpPdgBuilderVisitor.this.getVariableNode(var);
            PdgWriteEdge.create(node, varNode, "set", rhs);
            return null;
          }

          @Override
          public Void visit(ArrayIndexingNode arrayIndexingNode) {
            Variable arrayVar = arrayIndexingNode.getArray();
            PdgNode<ImpAstNode> varNode = ImpPdgBuilderVisitor.this.getVariableNode(arrayVar);
            PdgWriteEdge.create(node, varNode, "set", arrayIndexingNode.getIndex(), rhs);
            return null;
          }
        });

    return addNode(name, node, assignNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(SendNode sendNode) {
    ExpressionNode sentExpr = sendNode.getSentExpression();

    Set<Variable> temps = this.tempSetVisitor.run(sentExpr);
    Set<ReferenceNode> queries = this.querySetVisitor.run(sentExpr);

    // there should be NO queries for assignments in A-normal form!
    assert queries.size() == 0;

    String name = this.nameGenerator.getFreshName(SEND_NODE);
    PdgComputeNode<ImpAstNode> node = new PdgComputeNode<>(sendNode, name);

    createReadEdges(temps, queries, node);

    return addNode(name, node, sendNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(ReceiveNode recvNode) {
    String name = this.nameGenerator.getFreshName(RECV_NODE);
    PdgComputeNode<ImpAstNode> node = new PdgComputeNode<>(recvNode, name);

    PdgNode<ImpAstNode> varNode = this.getVariableNode(recvNode.getVariable());
    PdgWriteEdge.create(node, varNode, "set");

    return addNode(name, node, recvNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(IfNode ifNode) {
    String name = this.nameGenerator.getFreshName(IF_NODE);
    PdgControlNode<ImpAstNode> node = new PdgControlNode<>(ifNode, name);

    ExpressionNode guard = ifNode.getGuard();
    Set<Variable> temps = this.tempSetVisitor.run(guard);
    Set<ReferenceNode> queries = this.querySetVisitor.run(guard);
    createReadEdges(temps, queries, node);

    Set<PdgNode<ImpAstNode>> createdNodes = addNode(name, node, ifNode);
    Set<PdgNode<ImpAstNode>> thenNodes = ifNode.getThenBranch().accept(this);
    Set<PdgNode<ImpAstNode>> elseNodes = ifNode.getElseBranch().accept(this);
    Set<PdgNode<ImpAstNode>> branchNodes = new HashSet<>(thenNodes);
    branchNodes.addAll(elseNodes);
    createdNodes.addAll(branchNodes);

    Set<PdgNode<ImpAstNode>> readChannelNodes = new HashSet<>();
    for (PdgNode<ImpAstNode> branchNode : branchNodes) {
      PdgPcFlowEdge.create(node, branchNode);
      readChannelNodes.addAll(branchNode.getStorageNodeInputs());
    }

    for (PdgNode<ImpAstNode> readChannelNode : readChannelNodes) {
      PdgReadChannelEdge.create(node, readChannelNode);
    }

    // if the conditional is a loop guard, mark guard nodes
    if (ifNode.isLoopGuard()) {
      for (PdgNode<ImpAstNode> readNode : node.getReadNodes()) {
        if (readNode.isComputeNode()) {
          readNode.setLoopGuard(true);
        }
      }
    }

    return createdNodes;
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(WhileNode whileNode) {
    throw new ElaborationException();
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(LoopNode loopNode) {
    String name = this.nameGenerator.getFreshName(LOOP_NODE);
    PdgControlNode<ImpAstNode> node = new PdgControlNode<>(loopNode, name);

    Set<PdgNode<ImpAstNode>> createdNodes = addNode(name, node, loopNode);
    Set<PdgNode<ImpAstNode>> bodyNodes = loopNode.getBody().accept(this);
    createdNodes.addAll(bodyNodes);

    for (PdgNode<ImpAstNode> bodyNode : bodyNodes) {
      PdgPcFlowEdge.create(node, bodyNode);
    }

    return createdNodes;
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(BreakNode breakNode) {
    // TODO: figure out the edges we need to create here
    String name = this.nameGenerator.getFreshName(BREAK_NODE);
    PdgControlNode<ImpAstNode> node = new PdgControlNode<>(breakNode, name);
    return addNode(name, node, breakNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(BlockNode blockNode) {
    this.declaredVars.push();
    this.varDeclMap.push();

    Set<PdgNode<ImpAstNode>> createdNodes = new HashSet<>();
    for (StatementNode stmt : blockNode) {
      createdNodes.addAll(stmt.accept(this));
    }

    this.declaredVars.pop();
    this.varDeclMap.pop();

    return createdNodes;
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(AssertNode assertNode) {
    return new HashSet<>();
  }

  abstract class ReadSetVisitor<T> implements ReferenceVisitor<Set<T>>, ExprVisitor<Set<T>> {

    public Set<T> run(ExpressionNode expr) {
      return expr.accept(this);
    }

    public Set<T> run(ReferenceNode ref) {
      return ref.accept(this);
    }

    @Override
    public abstract Set<T> visit(Variable var);

    @Override
    public abstract Set<T> visit(ArrayIndexingNode arrayIndexingNode);

    @Override
    public Set<T> visit(ReadNode readNode) {
      return readNode.getReference().accept(this);
    }

    @Override
    public Set<T> visit(LiteralNode literalNode) {
      return new HashSet<>();
    }

    @Override
    public Set<T> visit(NotNode notNode) {
      return notNode.getExpression().accept(this);
    }

    @Override
    public Set<T> visit(BinaryExpressionNode binaryExpressionNode) {
      Set<T> lhsReads = binaryExpressionNode.getLhs().accept(this);
      Set<T> rhsReads = binaryExpressionNode.getRhs().accept(this);
      Set<T> reads = new HashSet<>();
      reads.addAll(lhsReads);
      reads.addAll(rhsReads);
      return reads;
    }

    @Override
    public Set<T> visit(DowngradeNode downgradeNode) {
      return downgradeNode.getExpression().accept(this);
    }
  }

  class TempSetVisitor extends ReadSetVisitor<Variable> {
    @Override
    public Set<Variable> visit(Variable var) {
      if (declaredVars.contains(var)) {
        return new HashSet<>();

      } else {
        Set<Variable> reads = new HashSet<>();

        if (ImpPdgBuilderVisitor.this.downgradeMap.containsKey(var)) {
          reads.add(ImpPdgBuilderVisitor.this.downgradeMap.get(var));

        } else {
          reads.add(var);
        }
        return reads;
      }
    }

    @Override
    public Set<Variable> visit(ArrayIndexingNode arrayIndexingNode) {
      return arrayIndexingNode.getIndex().accept(this);
    }
  }

  class QuerySetVisitor extends ReadSetVisitor<ReferenceNode> {
    @Override
    public Set<ReferenceNode> visit(Variable var) {
      if (declaredVars.contains(var)) {
        Set<ReferenceNode> reads = new HashSet<>();
        reads.add(var);
        return reads;

      } else {
        return new HashSet<>();
      }
    }

    @Override
    public Set<ReferenceNode> visit(ArrayIndexingNode arrayIndexingNode) {
      // we're assuming the AST is in A-normal form, so there's no need
      // to traverse the index since it's guaranteed to be an atomic expr
      Set<ReferenceNode> reads = new HashSet<>();
      reads.add(arrayIndexingNode);
      return reads;
    }
  }
}
