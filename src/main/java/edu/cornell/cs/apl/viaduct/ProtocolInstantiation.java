package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.AstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ProcessConfigurationBuilder;
import java.util.Map;

/** Given a protocol selection, instantiate a process configuration. */
public class ProtocolInstantiation<T extends AstNode> {
  /** Instantiate protocols for PDG nodes in the order given by control edges b/w PDG nodes. */
  public ProcessConfigurationNode instantiateProtocolConfiguration(
      HostTrustConfiguration hostConfig,
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>, Protocol<T>> protocolMap) {

    ProcessConfigurationBuilder pconfig = new ProcessConfigurationBuilder(hostConfig);
    ProtocolInstantiationInfo<T> info = new ProtocolInstantiationInfo<>(pconfig, protocolMap);
    for (PdgNode<T> node : pdg.getOrderedNodes()) {
      Protocol<T> proto = protocolMap.get(node);
      proto.instantiate(node, info);
    }

    return pconfig.build();
  }
}
