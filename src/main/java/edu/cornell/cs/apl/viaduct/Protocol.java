package edu.cornell.cs.apl.viaduct;

import java.util.Map;
import java.util.Set;

/** a protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  Set<Protocol<T>> createInstances(
      Set<Host> hostConfig, Map<PdgNode<T>, Protocol<T>> currProtoMap, PdgNode<T> node);

  Set<Host> getHosts();

  Set<Host> readFrom(PdgNode<T> node, Host h, ProtocolInstantiationInfo<T> info);

  Binding<T> readPostprocess(Map<Host,Binding<T>> hostBindings, Host readHost,
      ProtocolInstantiationInfo<T> info);

  void writeTo(PdgNode<T> node, Host writeHost, T val, ProtocolInstantiationInfo<T> info);

  void instantiate(PdgNode<T> node, ProtocolInstantiationInfo<T> info);
}
