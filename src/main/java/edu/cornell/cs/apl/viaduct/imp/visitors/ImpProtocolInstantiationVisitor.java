package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ProcessConfigurationBuilder;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph.ControlLabel;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCommunicationStrategy;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationError;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import java.util.Map;

/** instantiate process configuration from selected protocols. */
public class ImpProtocolInstantiationVisitor implements StmtVisitor<Void> {
  private final HostTrustConfiguration hostConfig;
  private final ProgramDependencyGraph<ImpAstNode> pdg;
  private final Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap;
  private final StatementNode main;
  private final ProcessConfigurationBuilder<ImpAstNode> pconfig;
  private final ProtocolInstantiationInfo<ImpAstNode> info;
  private final ImpProtocolInitializationVisitor initializer;

  /** constructor. */
  public ImpProtocolInstantiationVisitor(
      HostTrustConfiguration hostConfig,
      ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy,
      ProgramDependencyGraph<ImpAstNode> pdg,
      Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
      StatementNode main) {

    this.hostConfig = hostConfig;
    this.pdg = pdg;
    this.protocolMap = protocolMap;
    this.main = main;
    this.pconfig = new ProcessConfigurationBuilder<>(hostConfig);
    this.info =
        new ProtocolInstantiationInfo<>(
            hostConfig, communicationStrategy, pconfig, this.protocolMap);
    this.initializer = new ImpProtocolInitializationVisitor();
  }

  /** constructor. */
  public ProgramNode run() {
    this.main.accept(this.initializer);
    this.main.accept(this);
    return this.pconfig.build();
  }

  private Void visitSingleAstNode(String id) {
    PdgNode<ImpAstNode> node = this.pdg.getNode(id);
    Protocol<ImpAstNode> protocol = info.getProtocol(node);
    protocol.instantiate(node, info);
    return null;
  }

  @Override
  public Void visit(VariableDeclarationNode varDeclNode) {
    return visitSingleAstNode(varDeclNode.getId());
  }

  @Override
  public Void visit(ArrayDeclarationNode arrayDeclNode) {
    return visitSingleAstNode(arrayDeclNode.getId());
  }

  @Override
  public Void visit(LetBindingNode letBindingNode) {
    return visitSingleAstNode(letBindingNode.getId());
  }

  @Override
  public Void visit(AssignNode assignNode) {
    return visitSingleAstNode(assignNode.getId());
  }

  @Override
  public Void visit(SendNode sendNode) {
    throw new ProtocolInstantiationError("send not removed in PDG!");
  }

  @Override
  public Void visit(ReceiveNode receiveNode) {
    throw new ProtocolInstantiationError("recv not removed in PDG!");
  }

  @Override
  public Void visit(IfNode ifNode) {
    visitSingleAstNode(ifNode.getId());

    info.setCurrentPath(ControlLabel.THEN);
    ifNode.getThenBranch().accept(this);
    info.finishCurrentPath();

    info.setCurrentPath(ControlLabel.ELSE);
    ifNode.getElseBranch().accept(this);
    info.finishCurrentPath();

    info.popControlContext();
    return null;
  }

  @Override
  public Void visit(WhileNode whileNode) {
    throw new ElaborationException();
  }

  @Override
  public Void visit(ForNode forNode) {
    throw new ElaborationException();
  }

  @Override
  public Void visit(LoopNode loopNode) {
    visitSingleAstNode(loopNode.getId());

    info.setCurrentPath(ControlLabel.BODY);
    loopNode.getBody().accept(this);
    info.finishCurrentPath();

    info.popControlContext();
    info.popLoopControlContext();
    return null;
  }

  @Override
  public Void visit(BreakNode breakNode) {
    return visitSingleAstNode(breakNode.getId());
  }

  @Override
  public Void visit(BlockNode blockNode) {
    for (StatementNode stmt : blockNode) {
      stmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AssertNode assertNode) {
    throw new ProtocolInstantiationError("asserts should have been removed from PDG");
  }

  class ImpProtocolInitializationVisitor implements StmtVisitor<Void> {
    private Void visitSingleAstNode(String id) {
      PdgNode<ImpAstNode> node = pdg.getNode(id);
      Protocol<ImpAstNode> protocol = info.getProtocol(node);
      protocol.initialize(node, info);
      return null;
    }

    @Override
    public Void visit(VariableDeclarationNode varDeclNode) {
      return visitSingleAstNode(varDeclNode.getId());
    }

    @Override
    public Void visit(ArrayDeclarationNode arrayDeclNode) {
      return visitSingleAstNode(arrayDeclNode.getId());
    }

    @Override
    public Void visit(LetBindingNode letBindingNode) {
      return visitSingleAstNode(letBindingNode.getId());
    }

    @Override
    public Void visit(AssignNode assignNode) {
      return visitSingleAstNode(assignNode.getId());
    }

    @Override
    public Void visit(SendNode sendNode) {
      throw new ProtocolInstantiationError("send not removed in PDG!");
    }

    @Override
    public Void visit(ReceiveNode receiveNode) {
      throw new ProtocolInstantiationError("recv not removed in PDG!");
    }

    @Override
    public Void visit(IfNode ifNode) {
      visitSingleAstNode(ifNode.getId());
      ifNode.getThenBranch().accept(this);
      ifNode.getElseBranch().accept(this);
      return null;
    }

    @Override
    public Void visit(WhileNode whileNode) {
      throw new ElaborationException();
    }

    @Override
    public Void visit(ForNode forNode) {
      throw new ElaborationException();
    }

    @Override
    public Void visit(LoopNode loopNode) {
      visitSingleAstNode(loopNode.getId());
      loopNode.getBody().accept(this);
      return null;
    }

    @Override
    public Void visit(BreakNode breakNode) {
      return visitSingleAstNode(breakNode.getId());
    }

    @Override
    public Void visit(BlockNode blockNode) {
      for (StatementNode stmt : blockNode) {
        stmt.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(AssertNode assertNode) {
      throw new ProtocolInstantiationError("asserts should have been removed from PDG");
    }
  }
}
