package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.TargetPostprocessVisitor;

import java.util.Map;
import java.util.Set;

/** given a protocol selection, instantiate a process configuration. */
public class ProtocolInstantiation<T extends AstNode> {
  /** instantiate protocols for PDG nodes in the order given
   *  by control edges b/w PDG nodes. */
  public Map<Host,StmtNode> instantiateProtocolConfiguration(
      Set<Host> hostConfig, ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolMap) {

    ProcessConfigBuilder pconfig = new ProcessConfigBuilder(hostConfig);
    ProtocolInstantiationInfo<T> info =
        new ProtocolInstantiationInfo<>(pconfig, protocolMap);
    for (PdgNode<T> node : pdg.getOrderedNodes()) {
      Protocol<T> proto = protocolMap.get(node);
      proto.instantiate(node, info);
    }

    return pconfig.generateProcessConfig();
  }

  /** generate a single program containing process configuration. */
  public StmtNode instantiateProtocolSingleProgram(
      Set<Host> hostConfig, ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolMap) {

    ProcessConfigBuilder pconfig = new ProcessConfigBuilder(hostConfig);
    ProtocolInstantiationInfo<T> info =
        new ProtocolInstantiationInfo<>(pconfig, protocolMap);
    for (PdgNode<T> node : pdg.getOrderedNodes()) {
      Protocol<T> proto = protocolMap.get(node);
      proto.instantiate(node, info);
    }

    return pconfig.generateSingleProgram();
  }
}
