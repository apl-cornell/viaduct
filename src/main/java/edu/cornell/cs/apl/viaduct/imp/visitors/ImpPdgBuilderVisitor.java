package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.PdgBuilderInfo;
import edu.cornell.cs.apl.viaduct.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.PdgControlEdge;
import edu.cornell.cs.apl.viaduct.PdgControlNode;
import edu.cornell.cs.apl.viaduct.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.SymbolTable;
import edu.cornell.cs.apl.viaduct.UndeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
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
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * build program dependency graph from AST. the visit methods return the set of PDG node on which
 * the AST node depends on (reads)
 */
public class ImpPdgBuilderVisitor implements AstVisitor<PdgBuilderInfo<ImpAstNode>> {

  private static final String THEN_MARKER = "then";
  private static final String ELSE_MARKER = "else";
  private static final String DOWNGRADE_NODE = "downgrade";
  private static final String VARDECL_NODE = "decl";
  private static final String ASSIGN_NODE = "assign";
  private static final String IF_NODE = "if";

  private Map<String, Integer> freshVarMap;
  private SymbolTable<Variable, PdgNode<ImpAstNode>> storageNodes;
  private ProgramDependencyGraph<ImpAstNode> pdg;

  /** constructor that initializes to default "empty" state. */
  public ImpPdgBuilderVisitor() {}

  /** generate PDG from input program. */
  public ProgramDependencyGraph<ImpAstNode> generatePDG(StmtNode program) {
    this.freshVarMap = new HashMap<>();
    this.storageNodes = new SymbolTable<Variable, PdgNode<ImpAstNode>>();
    this.pdg = new ProgramDependencyGraph<ImpAstNode>();
    program.accept(this);

    // replace subexpressions with variables
    ReplaceVisitor replacer = new ReplaceVisitor();
    List<PdgNode<ImpAstNode>> orderedNodes = this.pdg.getOrderedNodes();
    Collections.reverse(orderedNodes);

    for (PdgNode<ImpAstNode> node : orderedNodes) {
      if (node.isComputeNode()) {
        for (PdgInfoEdge<ImpAstNode> inEdge : node.getInInfoEdges()) {
          PdgNode<ImpAstNode> inNode = inEdge.getSource();
          if (inNode.isComputeNode()) {
            ExpressionNode inExpr = (ExpressionNode) inNode.getAstNode();
            ReadNode read = new ReadNode(new Variable(inEdge.getLabel()));
            ImpAstNode newAst = replacer.replaceExpr(node.getAstNode(), inExpr, read);
            node.setAstNode(newAst);
          }
        }
      }
    }

    return this.pdg;
  }

  /** visit binary operation expr. */
  private PdgBuilderInfo<ImpAstNode> visitBinaryOp(BinaryExpressionNode binNode) {
    PdgBuilderInfo<ImpAstNode> lhsDeps = binNode.getLhs().accept(this);
    PdgBuilderInfo<ImpAstNode> rhsDeps = binNode.getRhs().accept(this);

    // add ordering b/w LHS and RHS
    PdgNode<ImpAstNode> leftLast = lhsDeps.getLastCreated();
    PdgNode<ImpAstNode> rightFirst = lhsDeps.getFirstCreated();
    if (leftLast != null && rightFirst != null) {
      PdgControlEdge.create(leftLast, rightFirst);
    }

    return lhsDeps.merge(rhsDeps);
  }

  private String getFreshName(String base) {
    Integer i = this.freshVarMap.get(base);
    if (i != null) {
      this.freshVarMap.put(base, i + 1);
      return String.format("%s_%d", base, i);

    } else {
      this.freshVarMap.put(base, 2);
      return base;
    }
  }

  /** return PDG storage node for referenced var. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(ReadNode varLookup) {
    if (this.storageNodes.contains(varLookup.getVariable())) {
      PdgNode<ImpAstNode> varNode = this.storageNodes.get(varLookup.getVariable());
      PdgBuilderInfo<ImpAstNode> deps = new PdgBuilderInfo<ImpAstNode>();
      deps.addReferencedNode(varNode, varLookup.getVariable());
      return deps;

    } else {
      throw new UndeclaredVariableException(varLookup.getVariable());
    }
  }

  /** return empty set of dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(IntegerLiteralNode intLit) {
    return new PdgBuilderInfo<ImpAstNode>();
  }

  /** return LHS and RHS dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(PlusNode plusNode) {
    return visitBinaryOp(plusNode);
  }

  /** return empty set of dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(BooleanLiteralNode boolLit) {
    return new PdgBuilderInfo<ImpAstNode>();
  }

  /** return LHS and RHS dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(OrNode orNode) {
    return visitBinaryOp(orNode);
  }

  /** return LHS and RHS dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(AndNode andNode) {
    return visitBinaryOp(andNode);
  }

  /** return LHS and RHS dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(LessThanNode ltNode) {
    return visitBinaryOp(ltNode);
  }

  /** return LHS and RHS dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(EqualNode eqNode) {
    return visitBinaryOp(eqNode);
  }

  /** return LHS and RHS dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(LeqNode leqNode) {
    return visitBinaryOp(leqNode);
  }

  /** return negated expr dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(NotNode notNode) {
    return notNode.getExpression().accept(this);
  }

  /** return created PDG node for downgrade. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(DowngradeNode downgradeNode) {
    PdgBuilderInfo<ImpAstNode> inInfo = downgradeNode.getExpression().accept(this);
    Label label = downgradeNode.getLabel();

    // create new PDG node
    // calculate inLabel later during dataflow analysis
    // AbstractLineNumber lineno = nextLineNumber();

    PdgNode<ImpAstNode> node =
        new PdgComputeNode<ImpAstNode>(
            downgradeNode, getFreshName(DOWNGRADE_NODE), Label.bottom(), label);

    inInfo.setReadNode(node);
    this.pdg.addNode(node);

    return new PdgBuilderInfo<>(node, new Variable(node.getId()));
  }

  /** return empty set of dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(SkipNode skipNode) {
    return new PdgBuilderInfo<>();
  }

  /** return created storage node. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(VarDeclNode varDecl) {
    // AbstractLineNumber lineno = nextLineNumber();
    Variable declVar = varDecl.getVariable();
    String nodeId = String.format("%s_%s", VARDECL_NODE, declVar.toString());
    PdgNode<ImpAstNode> node =
        new PdgStorageNode<ImpAstNode>(varDecl, getFreshName(nodeId), varDecl.getLabel());
    this.storageNodes.add(varDecl.getVariable(), node);
    this.pdg.addNode(node);

    return new PdgBuilderInfo<>(node);
  }

  /** return created PDG compute node for assignment. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(AssignNode assignNode) {
    if (this.storageNodes.contains(assignNode.getVariable())) {
      PdgBuilderInfo<ImpAstNode> inInfo = assignNode.getRhs().accept(this);
      PdgNode<ImpAstNode> varNode = this.storageNodes.get(assignNode.getVariable());

      // create new PDG node for the assignment that reads from the RHS nodes
      // and writes to the variable's storage node
      // AbstractLineNumber lineno = nextLineNumber();
      PdgNode<ImpAstNode> node =
          new PdgComputeNode<ImpAstNode>(assignNode, getFreshName(ASSIGN_NODE), Label.bottom());

      inInfo.setReadNode(node);
      PdgWriteEdge.create(node, varNode);
      this.pdg.addNode(node);

      PdgBuilderInfo<ImpAstNode> info = new PdgBuilderInfo<>(node);
      return inInfo.mergeCreated(info);

    } else {
      throw new UndeclaredVariableException(assignNode.getVariable());
    }
  }

  /** return dependencies of list of stmts. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(BlockNode blockNode) {
    PdgBuilderInfo<ImpAstNode> lastInfo = null;
    PdgBuilderInfo<ImpAstNode> blockInfo = new PdgBuilderInfo<>();

    for (StmtNode stmt : blockNode) {
      PdgBuilderInfo<ImpAstNode> curInfo = stmt.accept(this);

      if (lastInfo != null) {
        PdgNode<ImpAstNode> lastLastNode = lastInfo.getLastCreated();
        PdgNode<ImpAstNode> curFirstNode = curInfo.getFirstCreated();

        if (lastLastNode != null && curFirstNode != null) {
          PdgControlEdge.create(lastLastNode, curFirstNode);
        }
      }

      if (curInfo.getCreatedNodes().size() > 0) {
        lastInfo = curInfo;
        blockInfo = blockInfo.mergeCreated(curInfo);
      }
    }

    return blockInfo;
  }

  /** return created PDG compute node for conditional. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(IfNode ifNode) {
    // add edges from guard nodes
    PdgBuilderInfo<ImpAstNode> inInfo = ifNode.getGuard().accept(this);

    // AbstractLineNumber ifLineno = nextLineNumber();
    PdgNode<ImpAstNode> node =
        new PdgControlNode<ImpAstNode>(ifNode, getFreshName(IF_NODE), Label.bottom());
    inInfo.setReadNode(node);
    this.pdg.addNode(node);

    // add control edge to beginning of then block
    PdgBuilderInfo<ImpAstNode> thenInfo = ifNode.getThenBranch().accept(this);
    PdgNode<ImpAstNode> thenFirst = thenInfo.getFirstCreated();
    if (thenFirst != null) {
      PdgControlEdge.create(node, thenFirst, THEN_MARKER);
      PdgReadEdge.create(node, thenFirst);
    }

    // add control edge to beginning of else block
    PdgBuilderInfo<ImpAstNode> elseInfo = ifNode.getElseBranch().accept(this);
    PdgNode<ImpAstNode> elseFirst = elseInfo.getFirstCreated();
    if (elseFirst != null) {
      PdgControlEdge.create(node, elseFirst, ELSE_MARKER);
      PdgReadEdge.create(node, elseFirst);
    }

    // add read channel edges
    Set<PdgNode<ImpAstNode>> readChannelStorageSet = new HashSet<>();
    PdgBuilderInfo<ImpAstNode> branchInfo = thenInfo.merge(elseInfo);
    for (PdgNode<ImpAstNode> createdNode : branchInfo.getCreatedNodes()) {
      for (PdgNode<ImpAstNode> storage : createdNode.getStorageNodeInputs()) {
        readChannelStorageSet.add(storage);
      }
    }

    for (PdgNode<ImpAstNode> readChannelStorage : readChannelStorageSet) {
      PdgReadEdge.create(node, readChannelStorage);
    }

    PdgBuilderInfo<ImpAstNode> info = new PdgBuilderInfo<>(node);
    return inInfo.mergeCreated(info);
  }

  /** send/recvs should not be in surface programs and thus should not be in the generated PDG. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(SendNode sendNode) {
    return new PdgBuilderInfo<>();
  }

  /** send/recvs should not be in surface programs and thus should not be in the generated PDG. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(RecvNode recvNode) {
    return new PdgBuilderInfo<>();
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(AnnotationNode annotNode) {
    return new PdgBuilderInfo<>();
  }
}
