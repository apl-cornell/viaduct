package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationError;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** protocol for control structures. */
public class ControlProtocol extends Cleartext implements Protocol<ImpAstNode> {
  private static ControlProtocol instance = new ControlProtocol();

  private ControlProtocol() {
    super(new HashSet<>());
  }

  public static ControlProtocol getInstance() {
    return instance;
  }

  void instantiateControlNode(
      PdgControlNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

    ImpAstNode astNode = node.getAstNode();

    // TODO: this only works for single-level breaks for now
    if (astNode instanceof BreakNode) {
      Set<ProcessName> breakProcesses = info.getCurrentLoopControlContext();
      for (ProcessName breakProcess : breakProcesses) {
        StmtBuilder breakHostBuilder = info.getBuilder(breakProcess);
        breakHostBuilder.loopBreak((BreakNode) astNode);
      }

      return;
    }

    List<PdgReadEdge<ImpAstNode>> infoEdges = new ArrayList<>(node.getReadEdges());

    // create control structure in all nodes that have a read channel from the control node
    // TODO: this should really compute a transitive closure
    Set<ProcessName> controlStructureProcesses = new HashSet<>();
    for (PdgInfoEdge<ImpAstNode> infoEdge : node.getOutInfoEdges()) {
      PdgNode<ImpAstNode> target = infoEdge.getTarget();
      controlStructureProcesses.addAll(info.getProtocol(target).getProcesses());

      for (PdgNode<ImpAstNode> targetReadNode : target.getReadNodes()) {
        controlStructureProcesses.addAll(info.getProtocol(targetReadNode).getProcesses());
      }

      for (PdgInfoEdge<ImpAstNode> writeEdge : target.getWriteEdges()) {
        controlStructureProcesses.addAll(info.getProtocol(writeEdge.getTarget()).getProcesses());
      }
    }
    // TODO: this is a monkey patch...do an actual fix
    if (!info.isLoopControlContextEmpty()) {
      controlStructureProcesses.addAll(info.getCurrentLoopControlContext());
    }

    info.pushControlContext(controlStructureProcesses);

    if (astNode instanceof IfNode) {
      // conditional node should only have one read input (result of the guard)
      assert infoEdges.size() == 1;

      IfNode ifNode = (IfNode) astNode;
      for (ProcessName controlStructureProcess : controlStructureProcesses) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureProcess);
        Map<Variable, Variable> guardRenameMap =
            performComputeReads(controlStructureProcess, node, info);
        RenameVisitor guardRenamer = new RenameVisitor(guardRenameMap);
        IfNode newIfNode = (IfNode) guardRenamer.run(ifNode);
        controlStructureBuilder.pushIf(newIfNode);
      }

    } else if (astNode instanceof LoopNode) {
      info.pushLoopControlContext(controlStructureProcesses);
      LoopNode loopNode = (LoopNode) astNode;
      for (ProcessName controlStructureProcess : controlStructureProcesses) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureProcess);
        controlStructureBuilder.pushLoop(loopNode);
      }

    } else {
      throw new ProtocolInstantiationError("control node not associated with control structure");
    }
  }

  @Override
  public String getId() {
    return "Control";
  }

  @Override
  public Set<HostName> getHosts() {
    return new HashSet<>();
  }

  @Override
  public Set<ProcessName> getProcesses() {
    return new HashSet<>();
  }

  @Override
  public boolean hasSyntheticProcesses() {
    return false;
  }

  @Override
  public Label getTrust() {
    throw new ProtocolInstantiationError("control protocol has no associated trust");
  }

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {}

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    if (node.isControlNode()) {
      instantiateControlNode((PdgControlNode<ImpAstNode>) node, info);

    } else {
      throw new ProtocolInstantiationError(
          "control protocol must be associated with a control node");
    }
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> readNode,
      ProcessName readProcess,
      Binding<ImpAstNode> readLabel,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    throw new ProtocolInstantiationError("cannot read from a control protocol");
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      PdgNode<ImpAstNode> writeNode,
      ProcessName writeProcess,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    throw new ProtocolInstantiationError("cannot write to a control protocol");
  }

  @Override
  public String toString() {
    return "Control";
  }
}
