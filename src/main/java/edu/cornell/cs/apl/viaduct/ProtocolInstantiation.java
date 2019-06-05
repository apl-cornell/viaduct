package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ProcessConfigurationBuilder;
import java.util.List;
import java.util.Map;

/** given a protocol selection, instantiate a process configuration. */
public class ProtocolInstantiation<T extends AstNode> {
  private ProcessConfigurationBuilder getInstantiatedBuilder(
      HostTrustConfiguration hostConfig,
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>, Protocol<T>> protocolMap) {

    ProcessConfigurationBuilder pconfig = new ProcessConfigurationBuilder(hostConfig);
    ProtocolInstantiationInfo<T> info = new ProtocolInstantiationInfo<>(pconfig, protocolMap);

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

  /** Instantiate protocols for PDG nodes in the order given by control edges b/w PDG nodes. */
  public ProcessConfigurationNode instantiateProtocolConfiguration(
      HostTrustConfiguration hostConfig,
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>, Protocol<T>> protocolMap) {

    ProcessConfigurationBuilder pconfig = getInstantiatedBuilder(hostConfig, pdg, protocolMap);
    return pconfig.build();
  }
}
