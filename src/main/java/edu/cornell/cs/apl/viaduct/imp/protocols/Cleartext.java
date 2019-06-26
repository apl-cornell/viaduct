package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Cleartext {
  protected Variable instantiateStorageNode(
      Host host, PdgStorageNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

    // declare new variable
    VariableDeclarationNode varDecl = (VariableDeclarationNode) node.getAstNode();
    // Variable newVar = info.getFreshVar(varDecl.getVariable().getName());
    Variable newVar = varDecl.getVariable();

    StmtBuilder builder = info.getBuilder(host);
    builder.varDecl(newVar, varDecl.getType(), varDecl.getLabel());

    return newVar;
  }

  protected Binding<ImpAstNode> performRead(
      PdgNode<ImpAstNode> node,
      Binding<ImpAstNode> readLabel,
      Host host,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    StmtBuilder builder = info.getBuilder(host);
    Protocol<ImpAstNode> readNodeProto = info.getProtocol(node);
    Set<Host> readHosts = readNodeProto.readFrom(node, host, info);

    Map<Host, Binding<ImpAstNode>> hostBindings = new HashMap<>();
    for (Host readHost : readHosts) {
      Variable readVar = info.getFreshVar(readLabel);
      builder.recv(new ProcessName(readHost), readVar);
      hostBindings.put(readHost, readVar);
    }

    return readNodeProto.readPostprocess(hostBindings, host, info);
  }

  protected Variable instantiateComputeNode(
      Host host, PdgComputeNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

    StmtBuilder builder = info.getBuilder(host);
    // read from inputs
    Map<Variable, Variable> renameMap = new HashMap<>();
    for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
      Variable readVar =
          (Variable) performRead(readEdge.getSource(), readEdge.getLabel(), host, info);
      renameMap.put((Variable) readEdge.getLabel(), readVar);
    }

    // perform computation
    Variable outVar = info.getFreshVar(node.getId());
    ImpAstNode astNode = node.getAstNode();
    RenameVisitor renamer = new RenameVisitor(renameMap);
    if (astNode instanceof AssignNode) {
      AssignNode assignNode = (AssignNode) astNode;
      ExpressionNode computation = assignNode.getRhs().accept(renamer);
      builder.assign(outVar, computation);

    } else if (astNode instanceof ExpressionNode) {
      ExpressionNode computation = ((ExpressionNode) astNode).accept(renamer);
      builder.assign(outVar, computation);
    }

    // write to storage nodes
    ExpressionBuilder e = new ExpressionBuilder();
    for (PdgEdge<ImpAstNode> outEdge : node.getOutInfoEdges()) {
      // only write to variables, since if computations read from
      // the output of this node then it will call readFrom() anyway
      if (outEdge instanceof PdgWriteEdge<?>) {
        PdgStorageNode<ImpAstNode> outNode = (PdgStorageNode<ImpAstNode>) outEdge.getTarget();
        Protocol<ImpAstNode> outProto = info.getProtocol(outNode);
        outProto.writeTo(outNode, host, e.var(outVar), info);
      }
    }

    return outVar;
  }

  void instantiateControlNode(
      Set<Host> hosts,
      PdgControlNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // conditional node should only have one read input (result of the guard)
    List<PdgReadEdge<ImpAstNode>> infoEdges = new ArrayList<>(node.getReadEdges());
    assert infoEdges.size() == 1;

    // create conditional in all nodes that have a read channel from the control node
    Set<Host> controlStructureHosts = new HashSet<>(hosts);
    for (PdgInfoEdge<ImpAstNode> infoEdge : node.getOutInfoEdges()) {
      controlStructureHosts.addAll(info.getProtocol(infoEdge.getTarget()).getHosts());
    }

    info.pushControlContext(controlStructureHosts);

    ExpressionBuilder e = new ExpressionBuilder();
    PdgReadEdge<ImpAstNode> guardEdge = infoEdges.get(0);
    PdgNode<ImpAstNode> guardNode = guardEdge.getSource();
    Binding<ImpAstNode> guardLabel = guardEdge.getLabel();
    for (Host controlStructureHost : controlStructureHosts) {
      StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureHost);
      Binding<ImpAstNode> guardBinding =
          performRead(guardNode, guardLabel, controlStructureHost, info);
      controlStructureBuilder.pushIf(e.var((Variable) guardBinding));
    }
  }
}
