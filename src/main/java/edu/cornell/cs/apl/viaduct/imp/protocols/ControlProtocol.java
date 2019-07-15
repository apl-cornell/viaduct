package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationException;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** protocol for control structures. */
public class ControlProtocol extends Cleartext implements Protocol<ImpAstNode> {
  private static ControlProtocol instance = new ControlProtocol();

  private ControlProtocol() {}

  public static ControlProtocol getInstance() {
    return instance;
  }

  void instantiateControlNode(
      PdgControlNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    ImpAstNode astNode = node.getAstNode();

    // TODO: this only works for single-level breaks for now
    if (astNode instanceof BreakNode) {
      Set<Host> breakHosts = info.getCurrentLoopControlContext();
      for (Host breakHost : breakHosts) {
        StmtBuilder breakHostBuilder = info.getBuilder(breakHost);
        breakHostBuilder.loopBreak();
      }

      return;
    }

    List<PdgReadEdge<ImpAstNode>> infoEdges = new ArrayList<>(node.getReadEdges());

    // create control structure in all nodes that have a read channel from the control node
    // TODO: this should really compute a transitive closure
    Set<Host> controlStructureHosts = new HashSet<>();
    for (PdgInfoEdge<ImpAstNode> infoEdge : node.getOutInfoEdges()) {
      PdgNode<ImpAstNode> target = infoEdge.getTarget();
      controlStructureHosts.addAll(info.getProtocol(target).getHosts());

      for (PdgNode<ImpAstNode> targetReadNode : target.getReadNodes()) {
        controlStructureHosts.addAll(info.getProtocol(targetReadNode).getHosts());
      }

      for (PdgInfoEdge<ImpAstNode> writeEdge : target.getWriteEdges()) {
        controlStructureHosts.addAll(info.getProtocol(writeEdge.getTarget()).getHosts());
      }
    }

    info.pushControlContext(controlStructureHosts);

    if (astNode instanceof IfNode) {
      // conditional node should only have one read input (result of the guard)
      assert infoEdges.size() == 1;

      IfNode ifNode = (IfNode)astNode;
      for (Host controlStructureHost : controlStructureHosts) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureHost);
        Map<Variable,Variable> guardRenameMap =
            performComputeReads(controlStructureHost, node, info);
        RenameVisitor guardRenamer = new RenameVisitor(guardRenameMap);
        IfNode newIfNode = (IfNode)guardRenamer.run(ifNode);
        controlStructureBuilder.pushIf(newIfNode.getGuard());
      }

    } else if (astNode instanceof LoopNode) {
      info.pushLoopControlContext(controlStructureHosts);
      for (Host controlStructureHost : controlStructureHosts) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureHost);
        controlStructureBuilder.pushLoop();
      }

    } else {
      throw new ProtocolInstantiationException(
          "control node not associated with control structure");
    }
  }

  public Set<Host> getHosts() {
    return new HashSet<>();
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {}

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (node.isControlNode()) {
      instantiateControlNode((PdgControlNode<ImpAstNode>) node, info);

    } else {
      throw new ProtocolInstantiationException(
          "control protocol must be associated with a control node");
    }
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node,
      Host readHost,
      Binding<ImpAstNode> readLabel,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    throw new ProtocolInstantiationException("cannot read from a control protocol");
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      Host writeHost,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    throw new ProtocolInstantiationException("cannot write to a control protocol");
  }

  @Override
  public String toString() {
    return "Control";
  }
}
