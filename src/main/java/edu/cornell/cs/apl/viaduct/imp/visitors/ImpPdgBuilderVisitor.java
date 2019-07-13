package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
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
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
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
import edu.cornell.cs.apl.viaduct.security.Label;
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
  private static final String IF_NODE = "if";
  private static final String LOOP_NODE = "loop";
  private static final String BREAK_NODE = "break";

  private final FreshNameGenerator nameGenerator = new FreshNameGenerator();
  private final SymbolTable<Variable, Boolean> declaredVars;
  private final SymbolTable<Variable, String> varDeclMap;
  private final Map<String, PdgNode<ImpAstNode>> nodeMap;
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
  }

  /** generate a PDG from a program. */
  public ProgramDependencyGraph<ImpAstNode> generatePDG(StmtNode program) {
    this.declaredVars.clear();
    this.varDeclMap.clear();
    this.nodeMap.clear();
    this.pdg = new ProgramDependencyGraph<>();
    program.accept(this);
    return this.pdg;
  }

  private Set<PdgNode<ImpAstNode>> addNode(String name, PdgNode<ImpAstNode> node, StmtNode stmt) {
    this.pdg.addNode(node);
    this.nodeMap.put(name, node);
    stmt.setId(name);

    Set<PdgNode<ImpAstNode>> createdNodes = new HashSet<>();
    createdNodes.add(node);
    return createdNodes;
  }

  private void createReadEdges(
      Set<Variable> temps, Set<Reference> queries, PdgNode<ImpAstNode> node) {

    for (Variable temp : temps) {
      PdgNode<ImpAstNode> readNode = this.nodeMap.get(temp.getBinding());
      PdgComputeEdge.create(readNode, node, temp);
    }

    for (Reference query : queries) {
      Variable queryVar =
          query.accept(
              new ReferenceVisitor<Variable>() {
                @Override
                public Variable visit(Variable var) {
                  return var;
                }

                @Override
                public Variable visit(ArrayIndex arrayIndex) {
                  return arrayIndex.getArray();
                }
              });
      String readNodeName = this.varDeclMap.get(queryVar);
      PdgNode<ImpAstNode> readNode = this.nodeMap.get(readNodeName);
      PdgQueryEdge.create(readNode, node, ReadNode.create(query));
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
    PdgStorageNode<ImpAstNode> node = new PdgStorageNode<>(varDecl, name, varDecl.getLabel());

    this.varDeclMap.add(var, name);
    this.declaredVars.add(var, true);
    return addNode(name, node, varDecl);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(ArrayDeclarationNode arrayDecl) {
    Variable var = arrayDecl.getVariable();
    String name = this.nameGenerator.getFreshName(VARDECL_NODE);
    PdgStorageNode<ImpAstNode> node = new PdgStorageNode<>(arrayDecl, name, arrayDecl.getLabel());

    this.varDeclMap.add(var, name);
    this.declaredVars.add(var, true);

    ExpressionNode length = arrayDecl.getLength();
    Set<Variable> temps = this.tempSetVisitor.run(length);
    Set<Reference> queries = this.querySetVisitor.run(length);

    createReadEdges(temps, queries, node);

    return addNode(name, node, arrayDecl);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(LetBindingNode letBindingNode) {
    String name = letBindingNode.getVariable().getBinding();
    PdgComputeNode<ImpAstNode> node;
    ExpressionNode rhs = letBindingNode.getRhs();
    if (rhs instanceof DowngradeNode) {
      DowngradeNode downgradeNode = (DowngradeNode) rhs;
      node = new PdgComputeNode<>(downgradeNode, name, Label.weakest(), downgradeNode.getLabel());

    } else {
      node = new PdgComputeNode<>(rhs, name, Label.weakest());
    }

    Set<Variable> temps = this.tempSetVisitor.run(rhs);
    Set<Reference> queries = this.querySetVisitor.run(rhs);
    createReadEdges(temps, queries, node);

    // in A-normal form, there should be at most one query
    // assert queries.size() <= 1;

    return addNode(name, node, letBindingNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(AssignNode assignNode) {
    Reference lhs = assignNode.getLhs();
    ExpressionNode rhs = assignNode.getRhs();

    Set<Variable> lhsTemps = this.tempSetVisitor.run(lhs);
    Set<Variable> rhsTemps = this.tempSetVisitor.run(rhs);
    Set<Variable> temps = new HashSet<>(lhsTemps);
    temps.addAll(rhsTemps);

    Set<Reference> queries = new HashSet<>();
    /*
    Set<Reference> lhsQueries = this.querySetVisitor.run(lhs);
    Set<Reference> rhsQueries  = this.querySetVisitor.run(rhs);
    queries.addAll(lhsQueries);
    queries.addAll(rhsQueries);
    queries.remove(lhs);
    */

    // there should be NO queries for assignments in A-normal form!
    // assert queries.size() == 0;

    String name = this.nameGenerator.getFreshName(ASSIGN_NODE);
    PdgComputeNode<ImpAstNode> node = new PdgComputeNode<>(assignNode, name, Label.weakest());

    createReadEdges(temps, queries, node);

    // add write edge
    lhs.accept(
        new ReferenceVisitor<Set<PdgNode<ImpAstNode>>>() {
          @Override
          public Set<PdgNode<ImpAstNode>> visit(Variable var) {
            PdgNode<ImpAstNode> varNode = ImpPdgBuilderVisitor.this.getVariableNode(var);
            PdgWriteEdge.create(node, varNode, "set", rhs);
            return null;
          }

          @Override
          public Set<PdgNode<ImpAstNode>> visit(ArrayIndex arrayIndex) {
            Variable arrayVar = arrayIndex.getArray();
            PdgNode<ImpAstNode> varNode = ImpPdgBuilderVisitor.this.getVariableNode(arrayVar);
            PdgWriteEdge.create(node, varNode, "set", arrayIndex.getIndex(), rhs);
            return null;
          }
        });

    return addNode(name, node, assignNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(SendNode sendNode) {
    return new HashSet<>();
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(ReceiveNode receiveNode) {
    return new HashSet<>();
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(IfNode ifNode) {
    String name = this.nameGenerator.getFreshName(IF_NODE);
    PdgControlNode<ImpAstNode> node = new PdgControlNode<>(ifNode, name, Label.weakest());

    ExpressionNode guard = ifNode.getGuard();
    Set<Variable> temps = this.tempSetVisitor.run(guard);
    Set<Reference> queries = this.querySetVisitor.run(guard);
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
    PdgControlNode<ImpAstNode> node = new PdgControlNode<>(loopNode, name, Label.weakest());

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
    PdgControlNode<ImpAstNode> node = new PdgControlNode<>(breakNode, name, Label.weakest());
    return addNode(name, node, breakNode);
  }

  @Override
  public Set<PdgNode<ImpAstNode>> visit(BlockNode blockNode) {
    this.declaredVars.push();
    this.varDeclMap.push();

    Set<PdgNode<ImpAstNode>> createdNodes = new HashSet<>();
    for (StmtNode stmt : blockNode) {
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

    public Set<T> run(Reference ref) {
      return ref.accept(this);
    }

    @Override
    public abstract Set<T> visit(Variable var);

    @Override
    public abstract Set<T> visit(ArrayIndex arrayIndex);

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
        reads.add(var);
        return reads;
      }
    }

    @Override
    public Set<Variable> visit(ArrayIndex arrayIndex) {
      return arrayIndex.getIndex().accept(this);
    }
  }

  class QuerySetVisitor extends ReadSetVisitor<Reference> {
    @Override
    public Set<Reference> visit(Variable var) {
      if (declaredVars.contains(var)) {
        Set<Reference> reads = new HashSet<>();
        reads.add(var);
        return reads;

      } else {
        return new HashSet<>();
      }
    }

    @Override
    public Set<Reference> visit(ArrayIndex arrayIndex) {
      // we're assuming the AST is in A-normal form, so there's no need
      // to traverse the index since it's guaranteed to be an atomic expr
      Set<Reference> reads = new HashSet<>();
      reads.add(arrayIndex);
      return reads;
    }
  }
}
