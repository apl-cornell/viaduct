package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.UndeclaredVariableException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Reference;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgBuilderInfo;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgPcFlowEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadChannelEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Build program dependency graph from AST. The visit methods return the set of PDG nodes on which
 * the AST node depends (reads).
 */
public class ImpPdgBuilderVisitor
    implements ExprVisitor<PdgBuilderInfo<ImpAstNode>>,
        StmtVisitor<PdgBuilderInfo<ImpAstNode>>,
        ProgramVisitor<PdgBuilderInfo<ImpAstNode>> {

  private static final String DOWNGRADE_NODE = "downgrade";
  private static final String VARDECL_NODE = "decl";
  private static final String ASSIGN_NODE = "assgn";
  private static final String GUARD_NODE = "guard";
  private static final String IF_NODE = "if";

  private FreshNameGenerator freshNameGenerator;
  private SymbolTable<Variable, PdgNode<ImpAstNode>> storageNodes;
  private ProgramDependencyGraph<ImpAstNode> pdg;

  /** constructor that initializes to default "empty" state. */
  public ImpPdgBuilderVisitor() {}

  /** generate PDG from input program. */
  public ProgramDependencyGraph<ImpAstNode> generatePDG(StmtNode program) {
    this.freshNameGenerator = new FreshNameGenerator();
    this.storageNodes = new SymbolTable<>();
    this.pdg = new ProgramDependencyGraph<>();

    ElaborationVisitor elaborator = new ElaborationVisitor();
    program = elaborator.run(program);
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
            ImpAstNode newAst = replacer.run(node.getAstNode(), inExpr, read);
            node.setAstNode(newAst);
          }
        }
      }
    }

    return this.pdg;
  }

  /** Return empty set of dependencies. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(LiteralNode literalNode) {
    return new PdgBuilderInfo<>();
  }

  /** Return PDG storage node for referenced var. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(ReadNode readNode) {
    if (readNode.getReference() instanceof Variable) {
      Variable variable = (Variable) readNode.getReference();
      if (this.storageNodes.contains(variable)) {
        PdgNode<ImpAstNode> varNode = this.storageNodes.get(variable);
        PdgBuilderInfo<ImpAstNode> deps = new PdgBuilderInfo<>();
        deps.addReferencedNode(varNode, variable);
        return deps;
      } else {
        throw new UndeclaredVariableException(variable);
      }
    } else {
      // TODO: do the right thing
      return new PdgBuilderInfo<>();
    }
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(NotNode notNode) {
    return notNode.getExpression().accept(this);
  }

  /** visit binary operation expr. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(BinaryExpressionNode binNode) {
    PdgBuilderInfo<ImpAstNode> lhsDeps = binNode.getLhs().accept(this);
    PdgBuilderInfo<ImpAstNode> rhsDeps = binNode.getRhs().accept(this);

    // add ordering b/w LHS and RHS
    PdgNode<ImpAstNode> leftLast = lhsDeps.getLastCreated();
    PdgNode<ImpAstNode> rightFirst = lhsDeps.getFirstCreated();
    if (leftLast != null && rightFirst != null) {
      PdgControlEdge.create(leftLast, rightFirst, ControlLabel.SEQ);
    }

    return lhsDeps.merge(rhsDeps);
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(DowngradeNode downgradeNode) {
    PdgBuilderInfo<ImpAstNode> inInfo = downgradeNode.getExpression().accept(this);
    Label label = downgradeNode.getLabel();

    // create new PDG node
    // calculate inLabel later during dataflow analysis
    PdgNode<ImpAstNode> node =
        new PdgComputeNode<>(
            this.pdg,
            downgradeNode,
            this.freshNameGenerator.getFreshName(DOWNGRADE_NODE),
            Label.weakestPrincipal(),
            label);

    inInfo.setReadNode(node);
    this.pdg.addNode(node);

    return new PdgBuilderInfo<>(node, new Variable(node.getId()));
  }

  /** Return created storage node. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(VariableDeclarationNode variableDeclarationNode) {
    Variable declVar = variableDeclarationNode.getVariable();
    String nodeId = String.format("%s_%s", VARDECL_NODE, declVar.toString());
    PdgNode<ImpAstNode> node =
        new PdgStorageNode<>(
            this.pdg,
            variableDeclarationNode,
            this.freshNameGenerator.getFreshName(nodeId),
            variableDeclarationNode.getLabel());
    this.storageNodes.add(variableDeclarationNode.getVariable(), node);
    this.pdg.addNode(node);

    return new PdgBuilderInfo<>(node);
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(ArrayDeclarationNode arrayDeclarationNode) {
    // TODO: do the right thing
    return new PdgBuilderInfo<>();
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(LetBindingNode letBindingNode) {
    // TODO: do the right thing
    return new PdgBuilderInfo<>();
  }

  /** return created PDG compute node for assignment. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(AssignNode assignNode) {
    Reference lhs = assignNode.getLhs();

    if (lhs instanceof Variable) {
      Variable var = (Variable) lhs;
      if (this.storageNodes.contains(var)) {
        PdgBuilderInfo<ImpAstNode> inInfo = assignNode.getRhs().accept(this);
        PdgNode<ImpAstNode> varNode = this.storageNodes.get(var);

        // create new PDG node for the assignment that reads from the RHS nodes
        // and writes to the variable's storage node
        PdgNode<ImpAstNode> node =
            new PdgComputeNode<>(
                this.pdg,
                assignNode,
                this.freshNameGenerator.getFreshName(ASSIGN_NODE),
                Label.weakestPrincipal());

        inInfo.setReadNode(node);
        PdgWriteEdge.create(node, varNode);
        this.pdg.addNode(node);

        PdgBuilderInfo<ImpAstNode> info = new PdgBuilderInfo<>(node);
        return inInfo.mergeCreated(info);

      } else {
        throw new UndeclaredVariableException(var);
      }

    } else {
      // TODO: actually implement this
      return new PdgBuilderInfo<>();
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
          PdgControlEdge.create(lastLastNode, curFirstNode, ControlLabel.SEQ);
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
    PdgBuilderInfo<ImpAstNode> guardInfo = ifNode.getGuard().accept(this);

    // if there isn't exactly 1 created node from the guard, that means:
    // - there were no nodes created
    // - there were multiple nodes created
    // in either case, we want to create a new guard node
    if (guardInfo.getCreatedNodes().size() != 1) {
      PdgNode<ImpAstNode> guardNode =
          new PdgComputeNode<>(
              this.pdg,
              ifNode.getGuard(),
              this.freshNameGenerator.getFreshName(GUARD_NODE),
              Label.weakestPrincipal());
      guardInfo.setReadNode(guardNode);
      this.pdg.addNode(guardNode);
      guardInfo = new PdgBuilderInfo<>(guardNode, new Variable(guardNode.getId()));
    }

    PdgNode<ImpAstNode> controlNode =
        new PdgControlNode<>(
            this.pdg,
            ifNode,
            this.freshNameGenerator.getFreshName(IF_NODE),
            Label.weakestPrincipal());
    guardInfo.setReadNode(controlNode);
    this.pdg.addNode(controlNode);

    // add control edge to beginning of then block
    PdgBuilderInfo<ImpAstNode> thenInfo = ifNode.getThenBranch().accept(this);
    PdgNode<ImpAstNode> thenFirst = thenInfo.getFirstCreated();
    if (thenFirst != null) {
      PdgControlEdge.create(controlNode, thenFirst, ControlLabel.THEN);
    }

    // add control edge to beginning of else block
    PdgBuilderInfo<ImpAstNode> elseInfo = ifNode.getElseBranch().accept(this);
    PdgNode<ImpAstNode> elseFirst = elseInfo.getFirstCreated();
    if (elseFirst != null) {
      PdgControlEdge.create(controlNode, elseFirst, ControlLabel.ELSE);
    }

    // add read channel edges
    Set<PdgNode<ImpAstNode>> readChannelStorageSet = new HashSet<>();
    Set<PdgNode<ImpAstNode>> pcFlowSet = new HashSet<>();

    PdgBuilderInfo<ImpAstNode> branchInfo = thenInfo.merge(elseInfo);
    for (PdgNode<ImpAstNode> createdNode : branchInfo.getCreatedNodes()) {
      pcFlowSet.add(createdNode);
      readChannelStorageSet.addAll(createdNode.getStorageNodeInputs());
    }

    for (PdgNode<ImpAstNode> readChannelStorageNode : readChannelStorageSet) {
      PdgReadChannelEdge.create(controlNode, readChannelStorageNode);
    }

    for (PdgNode<ImpAstNode> pcFlowNode : pcFlowSet) {
      PdgPcFlowEdge.create(controlNode, pcFlowNode);
    }

    PdgBuilderInfo<ImpAstNode> info = new PdgBuilderInfo<>(controlNode);
    return guardInfo.mergeCreated(info);
  }

  /** return created PDG compute node for while loops. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(WhileNode whileNode) {
    // TODO: do the right thing
    return new PdgBuilderInfo<>();
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(ForNode forNode) {
    throw new Error(new ElaborationException());
  }

  /** send/recvs should not be in surface programs and thus should not be in the generated PDG. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(SendNode sendNode) {
    return new PdgBuilderInfo<>();
  }

  /** send/recvs should not be in surface programs and thus should not be in the generated PDG. */
  @Override
  public PdgBuilderInfo<ImpAstNode> visit(ReceiveNode receiveNode) {
    return new PdgBuilderInfo<>();
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(ProgramNode programNode) {
    throw new Error("Cannot build PDGs out of process configurations.");
  }

  @Override
  public PdgBuilderInfo<ImpAstNode> visit(AssertNode assertNode) {
    return new PdgBuilderInfo<>();
  }
}
