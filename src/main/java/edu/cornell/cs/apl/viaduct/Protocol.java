package edu.cornell.cs.apl.viaduct;

import java.util.Map;
import java.util.Set;

/** a protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  Set<Protocol<T>> createInstances(
      Set<Host> hostConfig, Map<PdgNode<T>, Protocol<T>> currProtoMap, PdgNode<T> node);

  Set<Host> readFrom(Host h, PdgNode<T> node, Map<PdgNode<T>,Protocol<T>> protocolMap,
      ProcessConfigBuilder pconfig);

  void writeTo(Host h, PdgNode<T> node, T val, Map<PdgNode<T>,Protocol<T>> protocolMap,
      ProcessConfigBuilder pconfig);

  void instantiate(PdgNode<T> node, Map<PdgNode<T>,Protocol<T>> protocolMap,
      ProcessConfigBuilder pconfig);
}
