package edu.cornell.cs.apl.viaduct;

import java.util.Map;
import java.util.Set;

/** a protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  Set<Protocol<T>> createInstances(
      Set<Host> hostConfig,
      Map<PdgNode<T>,Protocol<T>> currProtoMap,
      PdgNode<T> node);
}
