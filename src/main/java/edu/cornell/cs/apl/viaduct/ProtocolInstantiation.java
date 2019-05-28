package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;

import java.util.Map;
import java.util.Set;

/** given a protocol selection, instantiate a process configuration. */
public class ProtocolInstantiation<T extends AstNode> {
  /** instantiate protocols for PDG nodes in the order given
   *  by control edges b/w PDG nodes. */
  public Map<Host,StmtNode> instantiateProtocol(
      Set<Host> hostConfig, ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolSelection) {

    ProcessConfigBuilder builder = new ProcessConfigBuilder(hostConfig);
    for (PdgNode<T> node : pdg.getOrderedNodes()) {
      Protocol<T> proto = protocolSelection.get(node);
      proto.instantiate(node, protocolSelection, builder);
    }

    return builder.buildProcessConfig();
  }
}
