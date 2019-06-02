package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** given a protocol selection, instantiate a process configuration. */
public class ProtocolInstantiation<T extends AstNode> {
  private ProcessConfigBuilder getInstantiatedBuilder(
      Set<Host> hostConfig, ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolMap) {

    ProcessConfigBuilder pconfig = new ProcessConfigBuilder(hostConfig);
    ProtocolInstantiationInfo<T> info =
        new ProtocolInstantiationInfo<>(pconfig, protocolMap);

    for (PdgNode<T> node : pdg.getOrderedNodes()) {
      if (node.isStartOfControlFork()) {
        info.setCurrentPath(node.getInControlEdge().getLabel());
      }

      Protocol<T> proto = protocolMap.get(node);
      proto.instantiate(node, info);

      if (node.isEndOfExecutionPath() && !info.isControlContextEmpty()) {
        info.finishCurrentPath();
      }

      // check if node is the last one in the control structure;
      // if it is, pop the control structure out
      PdgControlNode<T> controlNode = node.getControlNode();
      if (controlNode != null) {
        // this is hilariously inefficient
        List<PdgNode<T>> controlChildren = controlNode.getControlStructureNodes();
        if (controlChildren.indexOf(node) == controlChildren.size() - 1) {
          info.popControl();
        }
      }
    }

    return pconfig;
  }

  /** instantiate protocols for PDG nodes in the order given
   *  by control edges b/w PDG nodes. */
  public Map<Host,StmtNode> instantiateProtocolConfiguration(
      Set<Host> hostConfig, ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolMap) {

    ProcessConfigBuilder pconfig = getInstantiatedBuilder(hostConfig, pdg, protocolMap);
    return pconfig.generateProcessConfig();
  }

  /** generate a single program containing process configuration. */
  public StmtNode instantiateProtocolSingleProgram(
      Set<Host> hostConfig, ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolMap) {

    ProcessConfigBuilder pconfig = getInstantiatedBuilder(hostConfig, pdg, protocolMap);
    return pconfig.generateSingleProgram();
  }
}
